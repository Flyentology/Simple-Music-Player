package com.simplemusicplayer;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Song implements Parcelable {

    private String songName;
    private String artistName;
    private String album;
    private long songID;
    private Bitmap albumArt;
    private String pathID;

    public Song(String songName, String artistName, String album, long songID, Bitmap albumArt, String pathID) {
        this.songName = songName;
        this.artistName = artistName;
        this.album = album;
        this.songID = songID;
        this.albumArt = albumArt;
        this.pathID = pathID;
    }

    public Song(String songName, String artistName, String album, long songID, String pathID){
        this.songName = songName;
        this.artistName = artistName;
        this.album = album;
        this.songID = songID;
        this.pathID = pathID;
        albumArt = null;
    }

    protected Song(Parcel in) {
        songName = in.readString();
        artistName = in.readString();
        album = in.readString();
        songID = in.readLong();
        albumArt = in.readParcelable(null);
        pathID = in.readString();
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

    public String getPathID() {
        return pathID;
    }

    public Bitmap getAlbumArt(){
        return albumArt;
    }

    public void setAlbumArt(Bitmap albumArt) {
        this.albumArt = albumArt;
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
        dest.writeParcelable(albumArt, 0);
        dest.writeString(pathID);
    }
}

