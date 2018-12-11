package com.codingwithmitch.audiostreamer.client;

import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaControllerCompat;

public interface CallbackCommand {
    void perform(@NonNull MediaControllerCompat.Callback callback);
}
