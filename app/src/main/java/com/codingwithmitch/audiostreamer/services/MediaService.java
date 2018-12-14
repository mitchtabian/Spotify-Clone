package com.codingwithmitch.audiostreamer.services;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class MediaService extends MediaBrowserServiceCompat {

    private static final String TAG = "MediaService";

    private MediaSessionCompat mSession;

    @Override
    public void onCreate() {
        super.onCreate();

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
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved: stopped");
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mSession.release();
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released");
    }


    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String s, int i, @Nullable Bundle bundle) {

        if(s.equals(getApplicationContext().getPackageName())){
            // Allowed to browse media
        }
        return new BrowserRoot("empty_media", null); // return no media
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "onLoadChildren: called: " + s + ", " + result);

        //  Browsing not allowed
        if (TextUtils.equals("empty_media", s)) {
            result.sendResult(null);
            return;
        }
        result.sendResult(null); // return all available media
    }


    public class MediaSessionCallback extends MediaSessionCompat.Callback{

        @Override
        public void onPrepare() {
            super.onPrepare();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPrepareFromMediaId(mediaId, extras);
        }

        @Override
        public void onPlay() {
            super.onPlay();
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            super.onAddQueueItem(description);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            super.onRemoveQueueItem(description);
        }
    }
}


















