package com.simplemusicplayer.models;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;


public class Song implements Parcelable {

    private String songName;
    private String artistName;
    private String album;
    private String path;
    private long songID;
    private boolean isSelected;
    private transient Bitmap coverArt;


    public Song(String songName, String artistName, String album, long songID, String path) {
        this.songName = songName;
        this.artistName = artistName;
        this.album = album;
        this.path = path;
        this.songID = songID;
    }

    public Song(String songName, String artistName, String album, long songID, Bitmap coverArt) {
        this.songName = songName;
        this.artistName = artistName;
        this.album = album;
        this.songID = songID;
        this.coverArt = coverArt;
    }

    protected Song(Parcel in) {
        songName = in.readString();
        artistName = in.readString();
        album = in.readString();
        songID = in.readLong();
        path = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public String getSongName() {
        return songName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbum() {
        return album;
    }

    public long getSongID() {
        return songID;
    }

    public Bitmap getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(Bitmap coverArt) {
        this.coverArt = coverArt;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public String toString() {
        return songName + " " + artistName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(songName);
        dest.writeString(artistName);
        dest.writeString(album);
        dest.writeLong(songID);
        dest.writeString(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return songID == song.songID;
    }

    @Override
    public int hashCode() {

        return Objects.hash(songID);
    }
}

