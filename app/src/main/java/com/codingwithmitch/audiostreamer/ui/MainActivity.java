package com.codingwithmitch.audiostreamer.ui;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.codingwithmitch.audiostreamer.R;


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
        testCategoryFragment();
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


















