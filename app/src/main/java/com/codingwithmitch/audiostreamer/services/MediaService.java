package com.codingwithmitch.audiostreamer.services;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.codingwithmitch.audiostreamer.MyApplication;
import com.codingwithmitch.audiostreamer.R;
import com.codingwithmitch.audiostreamer.client.MediaBrowserHelper;
import com.codingwithmitch.audiostreamer.notifications.MediaNotificationManager;
import com.codingwithmitch.audiostreamer.players.MediaPlayerAdapter;
import com.codingwithmitch.audiostreamer.players.PlaybackInfoListener;
import com.codingwithmitch.audiostreamer.players.PlayerAdapter;
import com.codingwithmitch.audiostreamer.util.MyPreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.EMPTY_MEDIA;
import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.MEDIA_QUEUE_POSITION;
import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.PLAYLIST_IDENTIFIER;
import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.QUEUE_NEW_PLAYLIST;
import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.SEEK_BAR_MAX;
import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.SEEK_BAR_PROGRESS;


public class MediaService extends MediaBrowserServiceCompat {

    private static final String TAG = "MediaService";


    private MediaSessionCompat mSession;
    private MediaNotificationManager mMediaNotificationManager;
    private boolean mServiceInStartedState;
    private PlayerAdapter mPlayback;
    private MyPreferenceManager mPreferenceManager;
    private MyApplication mMyApplication;


    @Override
    public void onCreate() {
        super.onCreate();

        mMyApplication = MyApplication.getInstance();

        //Build the MediaSession
        mSession = new MediaSessionCompat(this, TAG);


        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                // https://developer.android.com/guide/topics/media-apps/mediabuttons#mediabuttons-and-active-mediasessions
                // Media buttons on the device
                // (handles the PendingIntents for MediaButtonReceiver.buildMediaButtonPendingIntent)

                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS); // Control the items in the queue (aka playlist)
        // See https://developer.android.com/guide/topics/media-apps/mediabuttons for more info on flags

        mSession.setCallback(new MediaSessionCallback());

        // A token that can be used to create a MediaController for this session
        setSessionToken(mSession.getSessionToken());

        mMediaNotificationManager = new MediaNotificationManager(this);
        mPlayback = new MediaPlayerAdapter(this, new MediaPlayerListener());

        mPreferenceManager = new MyPreferenceManager(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved: stopped");
        super.onTaskRemoved(rootIntent);
        mPlayback.stop();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mSession.release();
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released");
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @NonNull Bundle bundle) {

        Log.d(TAG, "onGetRoot: CALLED: " + clientPackageName + ", " + clientUid + ", " + bundle.toString());
        String mediaSelector = bundle.getString(PLAYLIST_IDENTIFIER);

        if(mediaSelector != null){
            if(!mediaSelector.equals("")){
                return new BrowserRoot(mediaSelector, null); // return the playlist
            }
        }
        return new BrowserRoot(EMPTY_MEDIA, null); // return no media
    }



    /**
     * Sends the list of MediaBrowserCompat.MediaItem objects to "onChildrenLoaded" in MediaBrowserHelper
     * @param parentId
     * @param result
     */
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "onLoadChildren: CALLED: " + parentId + ", " + result);

        //  Browsing not allowed
        if (TextUtils.equals(EMPTY_MEDIA, parentId)) {
            result.sendResult(null);
            return;
        }
        for(MediaBrowserCompat.MediaItem item: mMyApplication.getMediaItems()){
            Log.d(TAG, "onLoadChildren: CALLED: item: " + item);
        }
        result.sendResult(mMyApplication.getMediaItems()); // return all available media
    }




    public class MediaSessionCallback extends MediaSessionCompat.Callback {

        private final List<MediaSessionCompat.QueueItem> mPlaylist = new ArrayList<>();
        private int mQueueIndex = -1;
        private MediaMetadataCompat mPreparedMedia;

        private void resetPlaylist(){
            mPlaylist.clear();
            mQueueIndex = -1;
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId: CALLED.");
            //Remove all previous queue items if new playlist
            if(extras.getBoolean(QUEUE_NEW_PLAYLIST, false)){
                resetPlaylist();
            }

            // Play the new media
            mPreparedMedia = mMyApplication.getTreeMap().get(mediaId);
            mSession.setMetadata(mPreparedMedia);
            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
            mPlayback.playFromMedia(mPreparedMedia);

            int newQueuePosition = extras.getInt(MEDIA_QUEUE_POSITION, -1);
            if(newQueuePosition == -1){
                mQueueIndex++;
            }
            else{
                mQueueIndex = extras.getInt(MEDIA_QUEUE_POSITION);
            }
            Log.d(TAG, "onPlayFromMediaId: called: playlist size: " + mPlaylist.size());
            Log.d(TAG, "onPlayFromMediaId: called: queue pos: " + mQueueIndex);
            mPreferenceManager.saveQueuePosition(mQueueIndex);
            mPreferenceManager.saveLastPlayedMedia(mPreparedMedia.getDescription().getMediaId());
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onAddQueueItem: CALLED: position in list: " + mPlaylist.size());
            mPlaylist.add(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            mPlaylist.remove(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mPlaylist.isEmpty()) ? -1 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }

        @Override
        public void onPrepare() {
            if (mQueueIndex < 0 && mPlaylist.isEmpty()) {
                // Nothing to play.
                return;
            }

            final String mediaId = mPlaylist.get(mQueueIndex).getDescription().getMediaId();
            mPreparedMedia = mMyApplication.getTreeMap().get(mediaId);
            mSession.setMetadata(mPreparedMedia);

            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
        }

        @Override
        public void onPlay() {

            if (!isReadyToPlay()) {
                // Nothing to play.
                return;
            }

            if (mPreparedMedia == null) {
                onPrepare();
            }

            mPlayback.playFromMedia(mPreparedMedia);

            Log.d(TAG, "onPlay: called: MediaSession active");

            mPreferenceManager.saveQueuePosition(mQueueIndex);
            mPreferenceManager.saveLastPlayedMedia(mPreparedMedia.getDescription().getMediaId());
        }

        @Override
        public void onPause() {
            mPlayback.pause();
        }

        @Override
        public void onStop() {
            mPlayback.stop();
            mSession.setActive(false);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext: SKIP TO NEXT");
            mQueueIndex = (++mQueueIndex % mPlaylist.size());
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious: SKIP TO PREVIOUS");
            mQueueIndex = mQueueIndex > 0 ? mQueueIndex - 1 : mPlaylist.size() - 1;
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onSeekTo(long pos) {
            mPlayback.seekTo(pos);
        }

        private boolean isReadyToPlay() {
            return (!mPlaylist.isEmpty());
        }
    }



    // MediaPlayerAdapter Callback: MediaPlayerAdapter state -> MusicService.
    public class MediaPlayerListener implements PlaybackInfoListener {

        private final ServiceManager mServiceManager;

        MediaPlayerListener() {
            mServiceManager = new ServiceManager();
        }

        @Override
        public void onSeekTo(long progress, long max) {
//            Log.d(TAG, "onSeekTo: CALLED: updating seekbar: " + progress + ", max: " + max);
            Intent intent = new Intent();
            intent.setAction(getString(R.string.broadcast_seekbar_update));
            intent.putExtra(SEEK_BAR_PROGRESS, progress);
            intent.putExtra(SEEK_BAR_MAX, max);
            sendBroadcast(intent);
        }

        @Override
        public void updateUI(String newMediaId) {
            Log.d(TAG, "updateUI: CALLED: " + newMediaId);
            Intent intent = new Intent();
            intent.setAction(getString(R.string.broadcast_update_ui));
            intent.putExtra(getString(R.string.broadcast_new_media_id), newMediaId);
            sendBroadcast(intent);
        }

        @Override
        public void onPlaybackComplete() {
            Log.d(TAG, "onPlaybackComplete: SKIPPING TO NEXT.");
            mSession.getController().getTransportControls().skipToNext();
        }

        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
            mSession.setPlaybackState(state);

            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    mServiceManager.updatePlayPauseState(state,
                            mPlayback.getCurrentMedia().getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI));
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    mServiceManager.updatePlayPauseState(state,
                            mPlayback.getCurrentMedia().getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI));
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    Log.d(TAG, "onPlaybackStateChange: STOPPED.");
                    mServiceManager.moveServiceOutOfStartedState();
                    break;
            }


        }

        class ServiceManager implements ICallback{

            private GetArtistBitmapAsyncTask mAsyncTask;
            private PlaybackStateCompat mState;
            private String mDisplayImageUri;
            private Bitmap mCurrentArtistBitmap;

            public ServiceManager() {

            }

            private void updatePlayPauseState(PlaybackStateCompat state, String displayImageUri){
                mState = state;
                if(!displayImageUri.equals(mDisplayImageUri)){
                    mAsyncTask = new GetArtistBitmapAsyncTask(Glide.
                            with(MediaService.this)
                            .asBitmap()
                            .load(mPlayback.getCurrentMedia().getDescription().getIconUri())
                            .listener(new RequestListener<Bitmap>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                    return true;
                                }
                            }).submit(),
                            this
                    );
                    mAsyncTask.execute();
                    mDisplayImageUri = displayImageUri;
                }
               else{
                    displayNotification(mCurrentArtistBitmap);
                }

            }

            private void moveServiceOutOfStartedState() {
                stopForeground(true);
                stopSelf();
                mServiceInStartedState = false;
            }

            @Override
            public void done(Bitmap bm) {
                mCurrentArtistBitmap = bm;
                displayNotification(bm);
            }

            private void displayNotification(Bitmap bm){
                // Manage the started state of this service.
                Notification notification = null;
                switch (mState.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING:
                        notification =
                                mMediaNotificationManager.getNotification(
                                        mPlayback.getCurrentMedia(), mState, getSessionToken(), bm);

                        if (!mServiceInStartedState) {
                            ContextCompat.startForegroundService(
                                    MediaService.this,
                                    new Intent(MediaService.this, MediaService.class));
                            mServiceInStartedState = true;
                        }

                        startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                        stopForeground(false);
                        notification =
                                mMediaNotificationManager.getNotification(
                                        mPlayback.getCurrentMedia(), mState, getSessionToken(), bm);
                        mMediaNotificationManager.getNotificationManager()
                                .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
                        break;
                }
            }
        }

    }


    static class GetArtistBitmapAsyncTask extends AsyncTask<Void, Void, Bitmap>{

        private FutureTarget<Bitmap> bm;
        private ICallback mICallback;

        public GetArtistBitmapAsyncTask(FutureTarget<Bitmap> bm, ICallback iCallback) {
            this.bm = bm;
            this.mICallback = iCallback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                return bm.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mICallback.done(bitmap);
        }

    }



}

























