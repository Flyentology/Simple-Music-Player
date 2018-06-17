package com.simplemusicplayer;

public class Song {

    private String songName;
    private String artistName;
    private String album;
    private long songID;

    public Song(String songName, String artistName, String album, long songID) {
        this.songName = songName;
        this.artistName = artistName;
        this.album = album;
        this.songID = songID;
    }


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
}
