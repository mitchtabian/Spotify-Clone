package com.codingwithmitch.audiostreamer.ui;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.codingwithmitch.audiostreamer.MediaSeekBar;
import com.codingwithmitch.audiostreamer.R;


public class MediaControllerFragment extends Fragment implements
        View.OnClickListener
{


    private static final String TAG = "MediaControllerFragment";


    // UI Components
    private TextView mSongTitle;
    private ImageView mPlayPause;
    private MediaSeekBar mSeekBarAudio;

    // Vars
    



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_controller, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mSongTitle = view.findViewById(R.id.media_song_title);
        mPlayPause = view.findViewById(R.id.play_pause);
        mSeekBarAudio = view.findViewById(R.id.seekbar_audio);

        mPlayPause.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        
    }
}


















