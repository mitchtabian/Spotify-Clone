package com.codingwithmitch.audiostreamer.client;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.List;

import static com.codingwithmitch.audiostreamer.util.Constants.PLAYLIST_IDENTIFIER;

public class MediaBrowserHelper {

    private static final String TAG = "MediaBrowserHelper";

    private final Context mContext;
    private final Class<? extends MediaBrowserServiceCompat> mMediaBrowserServiceClass;

    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaController;

    private MediaBrowserConnectionCallback mMediaBrowserConnectionCallback;
    private final MediaBrowserSubscriptionCallback mMediaBrowserSubscriptionCallback;
    private MediaControllerCallback mMediaControllerCallback;
    private MediaBrowserHelperCallback mMediaBrowserCallback;
    private boolean mWasConfigurationChange;


    public MediaBrowserHelper(Context context, Class<? extends MediaBrowserServiceCompat> serviceClass) {
        mContext = context;
        mMediaBrowserServiceClass = serviceClass;

        mMediaBrowserConnectionCallback = new MediaBrowserConnectionCallback();
        mMediaBrowserSubscriptionCallback = new MediaBrowserSubscriptionCallback();
        mMediaControllerCallback = new MediaControllerCallback();
    }

    public void setMediaBrowserHelperCallback(MediaBrowserHelperCallback callback){
        mMediaBrowserCallback = callback;
    }

    // Receives callbacks from the MediaController and updates the UI state,
    // i.e.: Which is the current item, whether it's playing or paused, etc.
    private class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            Log.d(TAG, "onMetadataChanged: CALLED");
            if(mMediaBrowserCallback != null){
                mMediaBrowserCallback.onMetadataChanged(metadata);
            }
        }

        @Override
        public void onPlaybackStateChanged(@Nullable final PlaybackStateCompat state) {
            Log.d(TAG, "onPlaybackStateChanged: CALLED");
            if(mMediaBrowserCallback != null){
                mMediaBrowserCallback.onPlaybackStateChanged(state);
            }
        }

        // This might happen if the MusicService is killed while the Activity is in the
        // foreground and onStart() has been called (but not onStop()).
        @Override
        public void onSessionDestroyed() {
            onPlaybackStateChanged(null);
        }
    }

    public void subscribeToNewPlaylist(String currentPlaylistId, String newPlatlistId){
        if(!currentPlaylistId.equals("")){
            mMediaBrowser.unsubscribe(currentPlaylistId);
        }
        mMediaBrowser.subscribe(newPlatlistId, mMediaBrowserSubscriptionCallback);
    }


    public void onStart(boolean wasConfigurationChange) {
        mWasConfigurationChange = wasConfigurationChange;
        if (mMediaBrowser == null) {
            mMediaBrowser =
                    new MediaBrowserCompat(
                            mContext,
                            new ComponentName(mContext, mMediaBrowserServiceClass),
                            mMediaBrowserConnectionCallback,
                            null);
            mMediaBrowser.connect();
        }
        Log.d(TAG, "onStart: CALLED: Creating MediaBrowser, and connecting");
    }

    public void onStop() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
            mMediaController = null;
        }
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
            mMediaBrowser = null;
        }
        Log.d(TAG, "onStop: CALLED: Releasing MediaController, Disconnecting from MediaBrowser");
    }

    // Receives callbacks from the MediaBrowser when it has successfully connected to the
    // MediaBrowserService (MusicService).
    private class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        // Happens as a result of onStart().
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected: CALLED");
            try {
                // Get a MediaController for the MediaSession.
                mMediaController =
                        new MediaControllerCompat(mContext, mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallback);


            } catch (RemoteException e) {
                Log.d(TAG, String.format("onConnected: Problem: %s", e.toString()));
                throw new RuntimeException(e);
            }

            mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mMediaBrowserSubscriptionCallback);
            Log.d(TAG, "onConnected: CALLED: subscribing to: " + mMediaBrowser.getRoot());

            mMediaBrowserCallback.onMediaControllerConnected(mMediaController);
        }
    }



    // Receives callbacks from the MediaBrowser when the MediaBrowserService has loaded new media
    // that is ready for playback.
    public class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: CALLED: " + parentId + ", " + children.toString());

            if(!mWasConfigurationChange){
                for (final MediaBrowserCompat.MediaItem mediaItem : children) {
                    Log.d(TAG, "onChildrenLoaded: CALLED: queue item: " + mediaItem.getMediaId());
                    mMediaController.addQueueItem(mediaItem.getDescription());
                }
            }

        }
    }

    public MediaControllerCompat.TransportControls getTransportControls() {
        if (mMediaController == null) {
            Log.d(TAG, "getTransportControls: MediaController is null!");
            throw new IllegalStateException("MediaController is null!");
        }
        return mMediaController.getTransportControls();
    }

}


























