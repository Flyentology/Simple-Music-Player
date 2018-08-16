package com.simplemusicplayer.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.simplemusicplayer.SongUtils;
import com.simplemusicplayer.R;
import com.simplemusicplayer.activities.MainActivity;
import com.simplemusicplayer.models.Song;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


public class MediaPlayerHolder extends Service implements MediaPlayer.OnCompletionListener, Serializable {

    private List<Song> songsList = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private int songIterator = 0;
    private boolean playedOnce, shuffle = false;
    private List<Integer> previouslyPlayed;
    private int index = 0;
    private boolean isPlayingPlaylist;
    private boolean repeatSongs = false;
    private MediaSession mediaSession;
    private List<Song> lastPlayedSongs = new ArrayList<>();
    private Set<Song> songSet = new LinkedHashSet<>();
    private IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MediaPlayerHolder getService() {
            // Return this instance of LocalService so clients can call public methods
            return MediaPlayerHolder.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        this.previouslyPlayed = new ArrayList<>();
        this.mediaPlayer = new MediaPlayer();
        this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        this.mediaSession = new MediaSession(this, "Playback");
        this.mediaPlayer.setOnCompletionListener(this);

        //creating an instance of nested receiver
        NotificationReceiver notificationReceiver = new NotificationReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.addAction("PLAY_NEXT");
        intentFilter.addAction("PAUSE");
        intentFilter.addAction("PLAY_PREVIOUS");
        intentFilter.addAction("SHUFFLE");
        intentFilter.addAction("SORT_TYPE");
        intentFilter.addAction("PLAY_PLAYLIST");
        intentFilter.addAction("ABORT_PLAYBACK");
        intentFilter.addAction("PROGRESS_UPDATE");
        getApplicationContext().registerReceiver(notificationReceiver, intentFilter);
        return mBinder;
    }

    /*
    Called when we start a service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public List<Integer> getPreviouslyPlayed() {
        return previouslyPlayed;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public List<Song> getSongsList() {
        return songsList;
    }

    public void setSongsList(List<Song> songsList) {
        this.songsList.clear();
        this.songsList.addAll(songsList);
    }

    public void setSongIterator(int songIterator) {
        this.songIterator = songIterator;
    }

    public boolean isPlayingPlaylist() {
        return isPlayingPlaylist;
    }

    public void setPlayingPlaylist(boolean playingPlaylist) {
        isPlayingPlaylist = playingPlaylist;
    }

    //Load song and start playback
    public void loadMedia(long ID) {
        reset();
        Uri uri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ID);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepare();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        playedOnce = true;
        Intent refreshBar = new Intent("REFRESH");
        sendBroadcast(refreshBar);
        createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), R.drawable.ic_pause);
        // refresh fragment
        UpdateProgressBar updateProgressBar = new UpdateProgressBar(songsList.get(songIterator).getSongName(), songsList.get(songIterator).getArtistName(), songsList.get(songIterator).getPath());
        updateProgressBar.start();
        // save last played songs in shared preferences
        lastPlayedSongs(songsList.get(songIterator));
        sendBroadcast(new Intent("LAST_PLAYED_SONG"));
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //onCompletion is called on the beginning, condition won't allow it to play until someone choose song
        if (playedOnce) {
            if (repeatSongs) {
                loadMedia(songsList.get(songIterator).getSongID());
            } else {
                nextSong();
                loadMedia(songsList.get(songIterator).getSongID());
            }
        }
    }

    private void reset() {
        mediaPlayer.reset();
    }

    public void createNotification(String songName, int icon) {

        // preparing to add notifications
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playNextIntent = new Intent("PLAY_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 1, playNextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent("PAUSE");
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playPreviousIntent = new Intent("PLAY_PREVIOUS");
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 3, playPreviousIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "MusicID")
                .setSmallIcon(R.drawable.ic_launcher_background) //notification icon
                .setContentTitle("Simple Music Player")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(0)
                .setContentText("Currently playing: " + songName)
                .setOngoing(mediaPlayer.isPlaying())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) //visible on locked screen
                .addAction(R.drawable.ic_previous, "Previous", prevPendingIntent) // #0
                .addAction(icon, "Pause", pausePendingIntent)  // #1
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent)// #2
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().setMediaSession(MediaSessionCompat.Token.fromToken(mediaSession.getSessionToken())))
                .setContentIntent(pendingIntent); //on click go to app intent
        startForeground(1337, mBuilder.build());
        // notificationId is a unique int for each notification that you must define
    }

    public class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            switch (action) {
                case "PLAY_NEXT":
                    nextSong();
                    loadMedia(songsList.get(songIterator).getSongID());
                    break;
                case "PAUSE":
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        sendBroadcast(new Intent("ICON_PAUSE"));
                        createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), R.drawable.ic_start);
                        stopForeground(false);
                    } else {
                        mediaPlayer.start();
                        UpdateProgressBar updateProgressBar = new UpdateProgressBar(songsList.get(songIterator).getSongName(), songsList.get(songIterator).getArtistName(), songsList.get(songIterator).getPath());
                        updateProgressBar.start();
                        sendBroadcast(new Intent("ICON_RESUME"));
                        createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), R.drawable.ic_pause);
                    }
                    break;
                case "PLAY_PREVIOUS":
                    previousSong();
                    loadMedia(songsList.get(songIterator).getSongID());
                    break;
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    mediaPlayer.pause();
                    sendBroadcast(new Intent("ICON_PAUSE"));
                    createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), R.drawable.ic_start);
                    stopForeground(false);
                    break;
                case "SHUFFLE":
                    int playbackIcon;
                    //TODO: redo this part of logic
                    if (intent.getBooleanExtra("ShuffleBoolean", false)) {
                        if (intent.getIntExtra("SHUFFLE_SONGS", 0) == 1) {
                            setShuffle(true);
                            randomSong();
                            removeHistory();
                        } else {
                            setShuffle(true);
                            removeHistory();
                        }
                        playbackIcon = 0;
                    } else {
                        setShuffle(false);
                        removeHistory();
                        playbackIcon = 1;
                    }
                    repeatSongs = intent.getBooleanExtra("REPEAT_SONG", false);
                    if (repeatSongs) {
                        playbackIcon = 2;
                    }
                    SharedPreferences playbackSettings = getSharedPreferences("PLAYBACK_TYPE", Context.MODE_PRIVATE);
                    SharedPreferences.Editor mEditor = playbackSettings.edit();
                    mEditor.putInt("PLAYBACK_ICON", playbackIcon);
                    mEditor.apply();
                    break;
                case "PLAY_PLAYLIST":
                    int iterator = intent.getIntExtra("PLAYLIST_ITERATOR", 0);
                    setSongsList(intent.<Song>getParcelableArrayListExtra("PLAYLIST"));
                    setSongIterator(iterator);
                    previouslyPlayed.clear();
                    loadMedia(songsList.get(iterator).getSongID());
                    previouslyPlayed.add(iterator);
                    isPlayingPlaylist = true;
                    break;
                case "ABORT_PLAYBACK":
                    mediaPlayer.stop();
                    removeHistory();
                    songsList = SongUtils.fillSongList(getApplicationContext(), 0);
                    setSongIterator(0);
                    break;
                case "PROGRESS_UPDATE":
                    mediaPlayer.seekTo(intent.getIntExtra("PROGRESS", 0));
                    break;
            }
        }
    }

    void nextSong() {
        if (shuffle) {
            if (index < 0) {
                index = 0;
            }
            if (index <= previouslyPlayed.size() - 2) {
                index++;
                songIterator = previouslyPlayed.get(index);
            } else {
                Random r = new Random();
                songIterator = r.nextInt(songsList.size());
                previouslyPlayed.add(songIterator);
                index = previouslyPlayed.size() - 1;
            }
        } else if (songIterator < songsList.size() - 1) {
            songIterator++;
        } else {
            songIterator = 0;
        }
    }

    void previousSong() {
        if (shuffle) {
            if (index > 0) {
                index--;
                songIterator = previouslyPlayed.get(index);
            } else {
                Random r = new Random();
                songIterator = r.nextInt(songsList.size());
                previouslyPlayed.add(0, songIterator);
            }
        } else if (songIterator >= 1) {
            songIterator--;
        } else {
            songIterator = songsList.size() - 1;
        }
    }

    public void removeHistory() {
        previouslyPlayed.clear();
        index = 0;
    }


    public void randomSong() {
        Random r = new Random();
        songIterator = r.nextInt(songsList.size());
        previouslyPlayed.add(songIterator);
        long id = songsList.get(songIterator).getSongID();
        loadMedia(id);
    }

    class UpdateProgressBar extends Thread {

        private String songName;
        private String artistName;
        private String coverPath;

        UpdateProgressBar(String songName, String artistName, String coverPath) {
            this.songName = songName;
            this.artistName = artistName;
            this.coverPath = coverPath;
        }

        @Override
        public void run() {
            sendBroadcast(new Intent("ICON_RESUME"));
            // save song data in preferences so every fragment/activity with unregistered receiver can access it
            SharedPreferences mSettings = getSharedPreferences("SONG_DATA", Context.MODE_PRIVATE);
            SharedPreferences.Editor mEditor = mSettings.edit();
            mEditor.putBoolean("IS_PAUSED", false);
            mEditor.putString("COVER_PATH", coverPath);
            mEditor.putString("SONG_NAME", songName);
            mEditor.putString("ARTIST_NAME", artistName);
            mEditor.apply();
            Intent songData = new Intent("SONG_DATA");
            sendBroadcast(songData);

            while (mediaPlayer.isPlaying()) {
                int songPosition = mediaPlayer.getCurrentPosition();
                // save song progress and total duration in shared preferences
                mEditor.putInt("CURRENT_POSITION", songPosition);
                mEditor.putInt("TOTAL_DURATION", mediaPlayer.getDuration());
                // send current progress and total duration to listening fragment/activity
                Intent intent = new Intent("PROGRESS");
                intent.putExtra("TIME", songPosition);
                intent.putExtra("TOTAL_DURATION", mediaPlayer.getDuration());
                sendBroadcast(intent);
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {

                }
            }
            mEditor.putBoolean("IS_PAUSED", true);
            mEditor.apply();
            sendBroadcast(new Intent("ICON_PAUSE"));
        }
    }

    private void lastPlayedSongs(Song song) {
        // if statement to check if song is already in the list
        if (!songSet.contains(song)) {
            songSet.add(song);
            lastPlayedSongs.add(0, song);
        } else {
            // if true find it, add at index 0 and remove duplicate
            int index = lastPlayedSongs.indexOf(song);
            lastPlayedSongs.add(0, song);
            lastPlayedSongs.remove(index + 1);
        }
        writeJSON(lastPlayedSongs);
    }

    /*
     * Method that saves last played songs into JSON
     * in shared preferences under key LAST_PLAYED_LIST
     */
    public void writeJSON(List<Song> songs) {
        Gson mGson = new Gson();
        SharedPreferences mSettings = getSharedPreferences("LAST_PLAYED_SONGS", Context.MODE_PRIVATE);
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
}