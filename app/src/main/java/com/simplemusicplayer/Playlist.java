package com.simplemusicplayer;

import java.util.ArrayList;

public class Playlist {
    private ArrayList<Song> playlistSongs;
    private String name;

    public Playlist(String name) {
        this.name = name;
        this.playlistSongs = new ArrayList<>();
    }

    @Override
    public String toString() {
        return name;
    }
}
