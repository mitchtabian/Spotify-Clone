package com.codingwithmitch.audiostreamer.util;


import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class MediaLibrary {

    private static final String TAG = "MediaLibrary";

    public TreeMap<String, MediaMetadataCompat> mMediaMap = new TreeMap<>();
    public List<MediaMetadataCompat> mMediaList = new ArrayList<>();

    public MediaLibrary() {
        initMap();
    }

    private void initMap(){
        for(MediaMetadataCompat media: mMediaLibrary){
            String mediaId = media.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            mMediaMap.put(mediaId, media);
            mMediaList.add(media);
        }
    }

    public static List<MediaBrowserCompat.MediaItem> getPlaylistMedia(Set<String> mediaIds) {
        List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();

        // VERY INEFFICIENT WAY TO DO THIS (BUT I NEED TO BECAUSE THE DATA STRUCTURE ARE NOT IDEAL)
        // RETRIEVING DATA FROM A SERVER WOULD NOT POSE THIS ISSUE
        for(String id: mediaIds){
            for (MediaMetadataCompat metadata : mMediaLibrary) {
                if(id.equals(metadata.getDescription().getMediaId())){
                    result.add(
                            new MediaBrowserCompat.MediaItem(
                                    metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
                }
            }
        }


        return result;
    }

    public static List<MediaBrowserCompat.MediaItem> getMediaItems() {
        List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        for (MediaMetadataCompat metadata : mMediaLibrary) {
            result.add(
                    new MediaBrowserCompat.MediaItem(
                            metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        }
        return result;
    }

    public TreeMap<String, MediaMetadataCompat> getTreeMap(){
        return mMediaMap;
    }

    public MediaMetadataCompat[] getMediaLibrary(){
        return mMediaLibrary;
    }

    private static final MediaMetadataCompat[] mMediaLibrary = {
            new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "11111")
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Mitch Tabian & Jim Wilson")
                    .putString(
                            MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                            "https://codingwithmitch.s3.amazonaws.com/static/profile_images/default_avatar.jpg")
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "CodingWithMitch Podcast #1 - Jim Wilson")
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                            "http://content.blubrry.com/codingwithmitch/Interview_audio_online-audio-converter.com_.mp3")
                    .build(),

            new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "11112")
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Mitch Tabian & Justin Mitchel")
                    .putString(
                            MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                            "https://codingwithmitch.s3.amazonaws.com/static/profile_images/default_avatar.jpg")
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                            "CodingWithMitch Podcast #2 - Justin Mitchel")
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                            "http://content.blubrry.com/codingwithmitch/Justin_Mitchel_interview_audio_online-audio-converter.com_.mp3")
                    .build(),

            new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "11113")
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Mitch Tabian & Matt Tran")
                    .putString(
                            MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                            "https://codingwithmitch.s3.amazonaws.com/static/profile_images/default_avatar.jpg")
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                            "CodingWithMitch Podcast #3 - Matt Tran")
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                            "http://content.blubrry.com/codingwithmitch/Matt_Tran_Interview_online-audio-converter.com_.mp3")
                    .build(),

            new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "11114")
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Mitch Tabian")
                    .putString(
                            MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                            "https://codingwithmitch.s3.amazonaws.com/static/profile_images/default_avatar.jpg")
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                            "Some Random Test Audio")
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                            "https://s3.amazonaws.com/codingwithmitch-static-and-media/pluralsight/Processes+and+Threads/audio+test+1+(online-audio-converter.com).mp3")
                    .build(),


    };


}