package com.codingwithmitch.audiostreamer.ui;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codingwithmitch.audiostreamer.R;


public class MediaControllerFragment extends Fragment {


    private static final String TAG = "MediaControllerFragment";


    // UI Components


    // Vars



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_controller, container, false);
    }




}


















