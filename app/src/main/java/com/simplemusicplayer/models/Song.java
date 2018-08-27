package com.simplemusicplayer.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;


public class Song implements Parcelable {

    private String songName;
    private String artistName;
    private String album;
    private String pathToFile;
    private String path;
    private long songID;
    private boolean isSelected;

    public Song(String songName, String artistName, String album, long songID, String path, String pathToFile) {
        this.songName = songName;
        this.artistName = artistName;
        this.album = album;
        this.path = path;
        this.songID = songID;
        this.pathToFile = pathToFile;
    }

    protected Song(Parcel in) {
        songName = in.readString();
        artistName = in.readString();
        album = in.readString();
        songID = in.readLong();
        path = in.readString();
        pathToFile = in.readString();
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

    public String getPath() {
        return path;
    }

    public String getPathToFile() {
        return pathToFile;
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
        dest.writeString(pathToFile);
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

