package com.codingwithmitch.audiostreamer.ui;

import com.codingwithmitch.audiostreamer.models.Artist;

public interface IMainActivity {

    void showProgressBar();

    void hideProgressBar();

    void onCategorySelected(String category);

    void onArtistSelected(String category, Artist artist);
}
