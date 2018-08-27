package com.simplemusicplayer.models;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Playlist implements Parcelable {
    private ArrayList<Song> playlistSongs;
    private String name;
    private String pathToCover = null;

    public Playlist(String name) {
        this.name = name;
        this.playlistSongs = new ArrayList<>();
    }

    protected Playlist(Parcel in) {
        playlistSongs = in.createTypedArrayList(Song.CREATOR);
        name = in.readString();
        pathToCover = in.readString();
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel in) {
            return new Playlist(in);
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

    public ArrayList<Song> getPlaylistSongs() {
        return playlistSongs;
    }

    public String getName() {
        return name;
    }

    public String getPathToCover() {
        return pathToCover;
    }

    public void setPathToCover(String pathToCover) {
        this.pathToCover = pathToCover;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getCount(){
        return playlistSongs.size();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(playlistSongs);
        parcel.writeString(name);
        parcel.writeString(pathToCover);
    }
}

