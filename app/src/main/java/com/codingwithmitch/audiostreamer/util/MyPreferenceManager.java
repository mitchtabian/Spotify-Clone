package com.codingwithmitch.audiostreamer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;


public class MyPreferenceManager {

    private static final String TAG = "MyPreferenceManager";

    // SharedPreference Keys
    public static final String PLAYLIST_ID = "PLAYLIST_ID";
    public static final String NOW_PLAYING = "NOW_PLAYING";
    public static final String MEDIA_QUEUE_POSITION = "MEDIA_QUEUE_POSITION";
    public static final String SEEK_BAR_PROGRESS = "SEEK_BAR_PROGRESS";
    public static final String MEDIA_SAVE_QUEUE_POSITION = "MEDIA_SAVE_QUEUE_POSITION";
    public static final String PLAYLIST_IDENTIFIER = "PLAYLIST_IDENTIFIER";
    public static final String EMPTY_MEDIA = "EMPTY_MEDIA";
    public static final String QUEUE_NEW_PLAYLIST = "QUEUE_NEW_PLAYLIST";
    public static final String LAST_CATEGORY = "LAST_CATEGORY";
    public static final String LAST_ARTIST = "LAST_ARTIST";
    public static final String LAST_ARTIST_IMAGE = "LAST_ARTIST_IMAGE";


    public static final String SEEK_BAR_MAX = "SEEK_BAR_MAX";

    private SharedPreferences mPreferences;

    public MyPreferenceManager(Context mContext) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public String getPlaylistId(){
        return mPreferences.getString(PLAYLIST_ID, "");
    }

    public void savePlaylistId(String playlistId){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PLAYLIST_ID, playlistId);
        editor.apply();
    }


    public void saveQueuePosition(int position){
        Log.d(TAG, "saveQueuePosition: SAVING QUEUE INDEX: " + position);
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(MEDIA_QUEUE_POSITION, position);
        editor.apply();
    }

    public int getQueuePosition(){
        return mPreferences.getInt(MEDIA_QUEUE_POSITION, -1);
    }

    public void saveLastPlayedArtistImage(String url){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(LAST_ARTIST_IMAGE, url);
        editor.apply();
    }

    public String getLastPlayedArtistImage(){
        return  mPreferences.getString(LAST_ARTIST_IMAGE, "");
    }

    public String getLastPlayedArtist(){
        return  mPreferences.getString(LAST_ARTIST, "");
    }

    public String getLastCategory(){
        return  mPreferences.getString(LAST_CATEGORY, "");
    }

    public void saveLastPlayedMedia(String mediaId){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(NOW_PLAYING, mediaId);
        editor.apply();
    }

    public String getLastPlayedMedia(){
        return mPreferences.getString(NOW_PLAYING, "");
    }

    public void saveLastPlayedCategory(String category){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(LAST_CATEGORY, category);
        editor.apply();
    }

    public void saveLastPlayedArtist(String artist){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(LAST_ARTIST, artist);
        editor.apply();
    }

}




















