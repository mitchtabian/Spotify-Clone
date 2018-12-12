package com.codingwithmitch.audiostreamer.ui;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.codingwithmitch.audiostreamer.R;
import com.codingwithmitch.audiostreamer.models.Artist;


public class MainActivity extends AppCompatActivity implements IMainActivity
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

//        testHomeFragment();
//        testCategoryFragment();
        testPlaylistFragment();
    }

    private void testPlaylistFragment(){
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container,
                        PlaylistFragment.newInstance(
                                "Podcasts",
                                new Artist("CodingWithMitch",
                                        "https://assets.blubrry.com/coverart/orig/654497-584077.png",
                                        "m2BE0t4z0raEqqqgHXj4")
                        )).commit();
    }


    private void testCategoryFragment(){
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, CategoryFragment.newInstance("Podcasts")).commit();
    }

    private void testHomeFragment(){
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, HomeFragment.newInstance()).commit();
    }

    @Override
    public void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);
    }
}


















