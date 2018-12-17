package com.codingwithmitch.audiostreamer.ui;

import com.codingwithmitch.audiostreamer.models.Artist;

public interface IMainActivity {

    void hideProgressBar();

    void showPrgressBar();

    void onCategorySelected(String category);

    void onArtistSelected(String category, Artist artist);

    void setActionBarTitle(String title);

}
