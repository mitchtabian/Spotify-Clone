package com.codingwithmitch.audiostreamer.players;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.PriorityTaskManager;
import com.google.android.exoplayer2.util.Util;

/**
 * Exposes the functionality of the {ExoPlayer} and implements the {@link PlayerAdapter}
 * so that {@MainActivity} can control music playback.
 */
public final class MediaPlayerAdapter extends PlayerAdapter {

    private static final String TAG = "MediaPlayerAdapter";

    private static final BandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private final Context mContext;
    private String mMediaId;
    private PlaybackInfoListener mPlaybackInfoListener;
    private MediaMetadataCompat mCurrentMedia;
    private int mState;
    private boolean mCurrentMediaPlayedToCompletion;
    private long mStartTime;

    // ExoPlayer objects
    private SimpleExoPlayer mExoPlayer;
    private TrackSelector mTrackSelector;
    private DefaultRenderersFactory mRenderersFactory;
    private DataSource.Factory mDataSourceFactory;


    public MediaPlayerAdapter(Context context, PlaybackInfoListener listener) {
        super(context);
        mContext = context.getApplicationContext();
        mPlaybackInfoListener = listener;

    }

    /**
     * Once the {ExoPlayer} is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the {MainActivity} the {ExoPlayer} is
     * released. Then in the onStart() of the {MainActivity} a new {ExoPlayer}
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    private void initializeExoPlayer() {
//        initExoPlayerMethod1();
        initExoPlayerMethod2();
    }

    private void initExoPlayerMethod2(){
        if (mExoPlayer == null) {
            mTrackSelector = new DefaultTrackSelector();
            mRenderersFactory = new DefaultRenderersFactory(mContext);
            mDataSourceFactory = new DefaultDataSourceFactory(mContext, Util.getUserAgent(mContext, "AudioStreamer"));
            mExoPlayer = ExoPlayerFactory.newSimpleInstance(mRenderersFactory, mTrackSelector, new DefaultLoadControl());
            mExoPlayer.addListener(new ExoPlayerEventListener());
        }
    }

    private void initExoPlayerMethod1(){
        if (mExoPlayer == null) {

            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            mTrackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
            PriorityTaskManager taskManager = new PriorityTaskManager();
            taskManager.add(1);
            DefaultLoadControl loadControl = new DefaultLoadControl( // Load control for ExoPlayer v2.8.4
                    new DefaultAllocator(true, 64 * 1024),
                    100000, // min buffer ms
                    1000000, // max buffer ms
                    50,
                    1000,
                    C.LENGTH_UNSET,
                    true,
                    taskManager
            );
            mExoPlayer = ExoPlayerFactory.newSimpleInstance(mContext, mTrackSelector, loadControl);
            mDataSourceFactory = new DefaultDataSourceFactory(mContext, Util.getUserAgent(mContext, "AudioStreamer"));
            mExoPlayer.addListener(new ExoPlayerEventListener());
        }
    }

    private void release() {
        if (mExoPlayer!= null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    // Implements PlaybackControl.
    @Override
    public void playFromMedia(MediaMetadataCompat metadata) {
        mCurrentMedia = metadata;
        playFile(metadata);
        startTrackingPlayback();
    }

    @Override
    public MediaMetadataCompat getCurrentMedia() {
        return mCurrentMedia;
    }

    private void playFile(MediaMetadataCompat metaData) {
        String mediaId = metaData.getDescription().getMediaId();
        boolean mediaChanged = (mMediaId == null || !mediaId.equals(mMediaId));
        if (mCurrentMediaPlayedToCompletion) {
            // Last audio file was played to completion, the resourceId hasn't changed, but the
            // player was released, so force a reload of the media file for playback.
            mediaChanged = true;
            mCurrentMediaPlayedToCompletion = false;
        }
        if (!mediaChanged) {
            if (!isPlaying()) {
                play();
            }
            return;
        }
        else {
            release();
        }

        mMediaId = mediaId;

        initializeExoPlayer();

        try {
            MediaSource audioSource =
                    new ExtractorMediaSource.Factory(mDataSourceFactory)
                            .createMediaSource(Uri.parse(metaData.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)));
            mExoPlayer.prepare(audioSource);
            Log.d(TAG, "onPlayerStateChanged: PREPARE");

        } catch (Exception e) {
            throw new RuntimeException("Failed to play media uri: "
                    + metaData.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI), e);
        }

        play();
    }


    @Override
    public void onStop() {
        // Regardless of whether or not the ExoPlayer has been created / started, the state must
        // be updated, so that MediaNotificationManager can take down the notification.
        Log.d(TAG, "onStop: stopped");
        setNewState(PlaybackStateCompat.STATE_STOPPED);
        release();
    }


    @Override
    public boolean isPlaying() {
        return mExoPlayer != null && mExoPlayer.getPlayWhenReady();
    }

    @Override
    protected void onPlay() {
        if (mExoPlayer != null && !mExoPlayer.getPlayWhenReady()) {
            Log.d(TAG, "onPlayerStateChanged: PLAY WHEN READY");
            mExoPlayer.setPlayWhenReady(true);
            setNewState(PlaybackStateCompat.STATE_PLAYING);
        }
    }

    @Override
    protected void onPause() {
        if (mExoPlayer != null && mExoPlayer.getPlayWhenReady()) {
            mExoPlayer.setPlayWhenReady(false);
            setNewState(PlaybackStateCompat.STATE_PAUSED);
        }
    }

    @Override
    public void seekTo(long position) {
        if (mExoPlayer != null) {
            mExoPlayer.seekTo((int) position);

            // Set the state (to the current state) because the position changed and should
            // be reported to clients.
            setNewState(mState);
        }
    }

    @Override
    public void setVolume(float volume) {
        if (mExoPlayer != null) {
            mExoPlayer.setVolume(volume);
        }
    }


    // This is the main reducer for the player state machine.
    private void setNewState(@PlaybackStateCompat.State int newPlayerState) {
        mState = newPlayerState;

        // Whether playback goes to completion, or whether it is stopped, the
        // mCurrentMediaPlayedToCompletion is set to true.
        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            mCurrentMediaPlayedToCompletion = true;
        }

        final long reportPosition = mExoPlayer == null ? 0 : mExoPlayer.getCurrentPosition();
        publishStateBuilder(reportPosition);
    }

    private void publishStateBuilder(long reportPosition){
        final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(getAvailableActions());
        stateBuilder.setState(mState,
                reportPosition,
                1.0f,
                SystemClock.elapsedRealtime());
        mPlaybackInfoListener.onPlaybackStateChange(stateBuilder.build());
        mPlaybackInfoListener.updateUI(mMediaId);
    }

    /**
     * Set the current capabilities available on this session. Note: If a capability is not
     * listed in the bitmask of capabilities then the MediaSession will not handle it. For
     * example, if you don't want ACTION_STOP to be handled by the MediaSession, then don't
     * included it in the bitmask that's returned.
     */
    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        switch (mState) {
            case PlaybackStateCompat.STATE_STOPPED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE;
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                actions |= PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_SEEK_TO;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_STOP;
                break;
            default:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }


    public void startTrackingPlayback(){
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable(){
            @Override
            public void run() {
                if(isPlaying()){
                    mPlaybackInfoListener.onSeekTo(
                            mExoPlayer.getContentPosition(), mExoPlayer.getDuration()
                    );
                    handler.postDelayed(this, 100);
                }
                if(mExoPlayer.getContentPosition() >= mExoPlayer.getDuration()
                        && mExoPlayer.getDuration() > 0){
                    mPlaybackInfoListener.onPlaybackComplete();
                }
            }
        };
        handler.postDelayed(runnable, 100);
    }


    private class ExoPlayerEventListener implements Player.EventListener{

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState){
                case Player.STATE_ENDED:{
                    setNewState(PlaybackStateCompat.STATE_PAUSED);
                    break;
                }
                case Player.STATE_BUFFERING:{
                    Log.d(TAG, "onPlayerStateChanged: BUFFERING");
                    mStartTime = System.currentTimeMillis();
                    break;
                }
                case Player.STATE_IDLE:{

                    break;
                }
                case Player.STATE_READY:{
                    Log.d(TAG, "onPlayerStateChanged: READY");
                    Log.d(TAG, "onPlayerStateChanged: TIME ELAPSED: " + (System.currentTimeMillis() - mStartTime));
                    break;
                }
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onPositionDiscontinuity(int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onSeekProcessed() {

        }
    }
}