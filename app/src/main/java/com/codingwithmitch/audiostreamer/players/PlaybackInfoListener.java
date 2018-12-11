package com.codingwithmitch.audiostreamer.players;

import android.support.v4.media.session.PlaybackStateCompat;

public interface PlaybackInfoListener {

    void onPlaybackStateChange(PlaybackStateCompat state);

    void onSeekTo(long progress, long max);

    void onPlaybackComplete();

    void updateUI(String newMediaId);
}
