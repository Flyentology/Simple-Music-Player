package com.simplemusicplayer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MediaPlayerHolder extends Service implements MediaPlayer.OnCompletionListener, Serializable {

    private ArrayList<Song> songsList = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private int songIterator = 0;
    private boolean playedOnce, shuffle = false;
    private List<Integer> previouslyPlayed;
    private int index = 0;
    private boolean isPlayingPlaylist;
    private MediaSession mediaSession;
    private NotificationCompat.Builder mBuilder;
    private IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        MediaPlayerHolder getService() {
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
        getApplicationContext().registerReceiver(notificationReceiver, intentFilter);
        return mBinder;
    }

    /*
    Called when we start a service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification("Pause");
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

    public void setSongsList(ArrayList<Song> songsList) {
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
        updateNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName());
        // refresh fragment
        UpdateProgressBar updateProgressBar = new UpdateProgressBar(songsList.get(songIterator).getSongName(), songsList.get(songIterator).getArtistName());
        updateProgressBar.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //onCompletion is called on the beginning, condition won't allow it to play until someone choose song
        if (playedOnce) {
            nextSong();
            loadMedia(songsList.get(songIterator).getSongID());
        }
    }

    private void reset() {
        mediaPlayer.reset();
    }

    public void createNotification(String currentState) {

        // preparing to add notifications
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playNextIntent = new Intent();
        playNextIntent.setAction("PLAY_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 1, playNextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent();
        pauseIntent.setAction("PAUSE");
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playPreviousIntent = new Intent();
        playPreviousIntent.setAction("PLAY_PREVIOUS");
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 3, playPreviousIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this, "MusicID")
                .setSmallIcon(R.drawable.ic_launcher_background) //notification icon
                .setContentTitle("Simple Music Player")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(0)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) //visible on locked screen
                .addAction(R.drawable.ic_previous, "Previous", prevPendingIntent) // #0
                .addAction(R.drawable.ic_pause, currentState, pausePendingIntent)  // #1
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent)// #2
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().setMediaSession(MediaSessionCompat.Token.fromToken(mediaSession.getSessionToken())))
                .setContentIntent(pendingIntent); //on click go to app intent
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        startForeground(1337, mBuilder.build());
        // notificationId is a unique int for each notification that you must define
    }

    public void updateNotification(String songName) {

        mBuilder.setContentText("Currently playing: " + songName);
        mBuilder.setOngoing(mediaPlayer.isPlaying());
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
        mNotificationManager.notify(1337, mBuilder.build());
        if (mediaPlayer.isPlaying()) {
            startForeground(1337, mBuilder.build());
        } else {
            stopForeground(false);
        }

    }

    public class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                String action = intent.getAction();
                switch (action) {
                    case "PLAY_NEXT":
                        nextSong();
                        loadMedia(songsList.get(songIterator).getSongID());
                        Log.d("ddd", "controller is working");
                        break;
                    case "PAUSE":
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                            sendBroadcast(new Intent("ICON_PAUSE"));
                            updateNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName());
                        } else {
                            mediaPlayer.start();
                            UpdateProgressBar updateProgressBar = new UpdateProgressBar(true);
                            updateProgressBar.start();
                            sendBroadcast(new Intent("ICON_RESUME"));
                            updateNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName());
                        }
                        break;
                    case "PLAY_PREVIOUS":
                        previousSong();
                        loadMedia(songsList.get(songIterator).getSongID());
                        break;
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        mediaPlayer.pause();
                        break;
                    case "SHUFFLE":
                        setShuffle(intent.getBooleanExtra("ShuffleBoolean", false));
                        randomSong();
                        removeHistory();
                        break;
                    case "PLAY_PLAYLIST":
                        int iterator = intent.getIntExtra("PLAYLIST_ITERATOR", 0);
                        setSongsList(intent.<Song>getParcelableArrayListExtra("PLAYLIST"));
                        setSongIterator(iterator);
                        previouslyPlayed.clear();
                        loadMedia(songsList.get(iterator).getSongID());
                        previouslyPlayed.add(iterator);
                        isPlayingPlaylist = true;
                }
            } catch (Exception e) {

            }
        }
    }

    //TODO: allow to replay songs
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

    //TODO: save all previously played songs
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
        private boolean isResumed;

        UpdateProgressBar(String songName, String artistName) {
            this.songName = songName;
            this.artistName = artistName;
            this.isResumed = false;
        }

        UpdateProgressBar(boolean isResumed) {
            this.isResumed = isResumed;
        }

        @Override
        public void run() {
            sendBroadcast(new Intent("ICON_RESUME"));
            if (!isResumed) {
                Intent songData = new Intent("SONG_DATA");
                songData.putExtra("SONG_NAME", songName);
                songData.putExtra("ARTIST_NAME", artistName);
                sendBroadcast(songData);
            }
            while (mediaPlayer.isPlaying()) {
                int songPosition = mediaPlayer.getCurrentPosition();
                Intent intent = new Intent("PROGRESS");
                intent.putExtra("TIME", songPosition);
                intent.putExtra("TOTAL_DURATION", mediaPlayer.getDuration());
                sendBroadcast(intent);
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {

                }
            }
            sendBroadcast(new Intent("ICON_PAUSE"));
        }
    }
}
