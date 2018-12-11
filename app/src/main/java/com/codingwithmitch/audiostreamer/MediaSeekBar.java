package com.codingwithmitch.audiostreamer;

import android.content.Context;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

/**
 * SeekBar that can be used with a {@link MediaSessionCompat} to track and seek in playing
 * media.
 */

public class MediaSeekBar extends AppCompatSeekBar {

    private static final String TAG = "MediaSeekBar";

    private MediaControllerCompat mMediaController;
    private ControllerCallback mControllerCallback;

    private boolean mIsTracking = false;

    private OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mIsTracking = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mMediaController.getTransportControls().seekTo(getProgress());
            mIsTracking = false;
        }
    };

    public boolean isTracking(){
        return mIsTracking;
    }

    public MediaSeekBar(Context context) {
        super(context);
        super.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    }

    public MediaSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    }

    public MediaSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    }

    @Override
    public final void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        // Prohibit adding seek listeners to this subclass.
        throw new UnsupportedOperationException("Cannot add listeners to a MediaSeekBar");
    }

    public void setMediaController(final MediaControllerCompat mediaController) {
        if (mediaController != null) {
            mControllerCallback = new ControllerCallback();
            mediaController.registerCallback(mControllerCallback);
        } else if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mControllerCallback = null;
        }
        mMediaController = mediaController;
    }

    public void disconnectController() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mControllerCallback = null;
            mMediaController = null;
        }
    }

    private class ControllerCallback
            extends MediaControllerCompat.Callback {

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }


        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            Log.d(TAG, "onPlaybackStateChanged: CALLED: playback state: " + state);

            final int progress = state != null
                    ? (int) state.getPosition()
                    : 0;
            setProgress(progress);

        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }

    }
}
