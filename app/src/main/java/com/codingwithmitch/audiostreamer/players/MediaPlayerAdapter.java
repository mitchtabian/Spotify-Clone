package com.codingwithmitch.audiostreamer.players;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class MediaPlayerAdapter extends PlayerAdapter {


    private static final String TAG = "MediaPlayerAdapter";

    
    private final Context mContext;


    // ExoPlayer objects
    private SimpleExoPlayer mExoPlayer;
    private TrackSelector mTrackSelector;
    private DefaultRenderersFactory mRenderersFactory;
    private DataSource.Factory mDataSourceFactory;


    public MediaPlayerAdapter(Context context) {
        super(context);
        mContext = context.getApplicationContext();

    }

    private void initializeExoPlayer(){
        if (mExoPlayer == null) {
            mTrackSelector = new DefaultTrackSelector();
            mRenderersFactory = new DefaultRenderersFactory(mContext);
            mDataSourceFactory = new DefaultDataSourceFactory(mContext, Util.getUserAgent(mContext, "AudioStreamer"));
            mExoPlayer = ExoPlayerFactory.newSimpleInstance(mRenderersFactory, mTrackSelector, new DefaultLoadControl());
        }
    }


    private void release() {
        if (mExoPlayer!= null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    @Override
    protected void onPlay() {

    }

    @Override
    protected void onPause() {

    }

    @Override
    public void playFromMedia(MediaMetadataCompat metadata) {

    }

    @Override
    public MediaMetadataCompat getCurrentMedia() {
        return null;
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    protected void onStop() {

    }

    @Override
    public void seekTo(long position) {

    }

    @Override
    public void setVolume(float volume) {

    }
}
