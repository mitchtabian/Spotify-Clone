package com.codingwithmitch.audiostreamer.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Artist implements Parcelable {

    private String title;
    private String image;
    private String artist_id;

    public Artist(String title, String image, String artist_id) {
        this.title = title;
        this.image = image;
        this.artist_id = artist_id;
    }

    public Artist() {
    }

    protected Artist(Parcel in) {
        title = in.readString();
        image = in.readString();
        artist_id = in.readString();
    }

    public static final Creator<Artist> CREATOR = new Creator<Artist>() {
        @Override
        public Artist createFromParcel(Parcel in) {
            return new Artist(in);
        }

        @Override
        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getArtist_id() {
        return artist_id;
    }

    public void setArtist_id(String artist_id) {
        this.artist_id = artist_id;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeString(image);
        parcel.writeString(artist_id);
    }
}
