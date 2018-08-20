package com.simplemusicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.simplemusicplayer.models.Song;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PlaybackLogic {

    private static List<Song> songsList = new ArrayList<>();
    private static List<Song> previouslyPlayed = new ArrayList<>();
    private static Set<Song> songSet = new LinkedHashSet<>();
    private static List<Song> lastPlayedSongs = new ArrayList<>();
    private static int songIterator = 0;
    private static int index = 0;
    private static boolean shuffle = false;
    private static boolean playAgain = false;
    private static boolean isPlayingPlaylist = false;

    public static List<Song> getSongsList() {
        return songsList;
    }

    public static void setSongsList(List<Song> songsList) {
        PlaybackLogic.songsList = songsList;
    }

    public static int getSongIterator() {
        return songIterator;
    }

    public static void setSongIterator(int songIterator) {
        PlaybackLogic.songIterator = songIterator;
    }

    public static int getIndex() {
        return index;
    }

    public static void setIndex(int index) {
        PlaybackLogic.index = index;
    }

    public static boolean isShuffle() {
        return shuffle;
    }

    public static void setShuffle(boolean shuffle) {
        PlaybackLogic.shuffle = shuffle;
    }

    public static void setPlayAgain(boolean playAgain) {
        PlaybackLogic.playAgain = playAgain;
    }

    public static boolean getPlayAgain() {
        return playAgain;
    }

    public static List<Song> getPreviouslyPlayed() {
        return previouslyPlayed;
    }

    public static void setPreviouslyPlayed(List<Song> previouslyPlayed) {
        PlaybackLogic.previouslyPlayed = previouslyPlayed;
    }

    public static boolean isPlayingPlaylist() {
        return isPlayingPlaylist;
    }

    public static void setPlayingPlaylist(boolean isPlayingPlaylist) {
        PlaybackLogic.isPlayingPlaylist = isPlayingPlaylist;
    }

    public static Song nextSong() {
        if (shuffle) {
            if (index < 0) {
                index = 0;
            }
            if (index <= previouslyPlayed.size() - 2) {
                index++;
                return previouslyPlayed.get(index);
            } else {
                Random r = new Random();
                songIterator = r.nextInt(songsList.size());
                previouslyPlayed.add(songsList.get(songIterator));
                index = previouslyPlayed.size() - 1;
                return songsList.get(songIterator);
            }
        } else if (songIterator < songsList.size() - 1) {
            songIterator++;
        } else {
            songIterator = 0;
        }
        return songsList.get(songIterator);
    }

    public static Song previousSong() {
        if (shuffle) {
            if (index > 0) {
                index--;
                return previouslyPlayed.get(index);
            } else {
                Random r = new Random();
                songIterator = r.nextInt(songsList.size());
                previouslyPlayed.add(0, songsList.get(songIterator));
                return songsList.get(songIterator);
            }
        } else if (songIterator >= 1) {
            songIterator--;
        } else {
            songIterator = songsList.size() - 1;
        }
        return songsList.get(songIterator);
    }

    public static void removeHistory() {
        previouslyPlayed.clear();
        index = 0;
    }


    public static void lastPlayedSongs(Song song, Context context) {
        // if statement to check if song is already in the list
        if (!songSet.contains(song)) {
            songSet.add(song);
            lastPlayedSongs.add(0, song);
        } else {
            // if true remove duplicate and add it at index 0
            lastPlayedSongs.remove(song);
            lastPlayedSongs.add(0, song);
        }
        writeJSON(lastPlayedSongs, context);
    }

    /*
     * Method that saves last played songs into JSON
     * in shared preferences under key LAST_PLAYED_LIST
     */
    public static void writeJSON(List<Song> songs, Context context) {
        Gson mGson = new Gson();
        SharedPreferences mSettings = context.getSharedPreferences("LAST_PLAYED_SONGS", Context.MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mSettings.edit();
        Type type = new TypeToken<List<Song>>() {
        }.getType();
        try {
            String writeValue = mGson.toJson(songs, type);
            mEditor.putString("LAST_PLAYED_LIST", writeValue);
            mEditor.apply();
        } catch (Exception e) {
        }
    }

    public static void readPreferences(Context context) {
        Gson mGson = new Gson();
        SharedPreferences mSettings = context.getSharedPreferences("LAST_PLAYED_PLAYLIST", Context.MODE_PRIVATE);
        Type collectionType = new TypeToken<List<Song>>() {
        }.getType();
        String loadValue = mSettings.getString("LAST_PLAYED_LIST", null);
        List<Song> playlistToPlay = mGson.fromJson(loadValue, collectionType);

        if (playlistToPlay != null) {
            Log.d("ddd", "not null");
            setSongsList(playlistToPlay);
            setSongIterator(mSettings.getInt("SONG_ITERATOR", 0));
        }
        Log.d("ddd", " "+ mSettings.getInt("SONG_ITERATOR", 0));
    }
}
