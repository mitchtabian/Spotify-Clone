package com.codingwithmitch.audiostreamer.ui;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;

import com.codingwithmitch.audiostreamer.R;


public class MainActivity extends AppCompatActivity
{

    private static final String TAG = "MainActivity";

    //UI Components
    private ProgressBar mProgressBar;

    // Vars


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = findViewById(R.id.progress_bar);


    }


}


















