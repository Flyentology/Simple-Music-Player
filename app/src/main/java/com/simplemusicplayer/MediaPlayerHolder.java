package com.simplemusicplayer;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MediaPlayerHolder implements MediaPlayer.OnCompletionListener, MediaController.MediaPlayerControl {


    private final Context mContext;
    private List<Song> songsList;
    private MediaPlayer mediaPlayer;
    private int songIterator = 0;
    private MediaController mediaController;
    private boolean playedOnce, shuffle = false;
    private List<Integer> previouslyPlayed;
    private MainActivity mainActivity;
    private int index = 0;

    public MediaPlayerHolder(Context mContext, MainActivity mainActivity) {
        this.mContext = mContext.getApplicationContext();
        this.songsList = new ArrayList<>();
        this.previouslyPlayed = new ArrayList<>();
        this.mediaPlayer = new MediaPlayer();
        this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        this.mediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        this.mediaPlayer.setOnCompletionListener(this);
        this.mediaController = new MediaController(mContext);
        this.mainActivity = mainActivity;
        //creating an instance of nested receiver
        NotificationReceiver notificationReceiver = new NotificationReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.addAction("PLAY_NEXT");
        intentFilter.addAction("PAUSE");
        intentFilter.addAction("PLAY_PREVIOUS");
        mContext.registerReceiver(notificationReceiver, intentFilter);
    }

    public List<Integer> getPreviouslyPlayed() {
        return previouslyPlayed;
    }

    public MediaController getMediaController() {
        return mediaController;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public void setMediaController() {
        mediaController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextSong();
                loadMedia(songsList.get(songIterator).getSongID());

            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousSong();
                loadMedia(songsList.get(songIterator).getSongID());
            }
        });
        mediaController.setMediaPlayer(this);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public List<Song> getSongsList() {
        return songsList;
    }

    public int getSongIterator() {
        index = previouslyPlayed.size() - 1;
        return songIterator;
    }

    public void setSongIterator(int songIterator) {
        this.songIterator = songIterator;
    }

    public void fillSongList() {
        ContentResolver contentResolver = mContext.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; //uri is basically URL, so points to a place in the phone where media is stored
        Cursor cursor = contentResolver.query(uri, null, null, null, null); //query for audio files on the phone
        if (cursor == null) {
            // query failed
            Log.d("Cursors ", "query failed");
        } else if (!cursor.moveToFirst()) {
            //no media on the device
            Log.d("Media", "no media on the device");
        } else {
            // get index of each parameter of audio file
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            do {
                long id = cursor.getLong(idColumn);
                String thisTitle = cursor.getString(titleColumn);
                String artistName = cursor.getString(artistColumn);
                String albumName = cursor.getString(albumColumn);
                songsList.add(new Song(thisTitle, artistName, albumName, id));
            } while (cursor.moveToNext());
            cursor.close(); //cursors should be freed up after use
        }
    }


    //Load song and start playback
    public void loadMedia(long ID) {
        reset();
        Uri uri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ID);
        try {
            mediaPlayer.setDataSource(mContext.getApplicationContext(), uri);
            mediaPlayer.prepare();
            if (isPlaying()) {
                pause();
            } else {
                start();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        playedOnce = true;
        mainActivity.createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), "Pause", isPlaying());
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //onCompletion is called on the beginning, condition won't allow it to play until someone choose song
        if (playedOnce) {
            nextSong();
            loadMedia(songsList.get(songIterator).getSongID());
        }
    }

    @Override
    public void start() {
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public void reset() {
        mediaPlayer.reset();
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
                        break;
                    case "PAUSE":
                        if (isPlaying()) {
                            pause();
                            mainActivity.createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), "Resume", isPlaying());
                        } else {
                            start();
                            mainActivity.createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), "Pause", isPlaying());
                        }
                        break;
                    case "PLAY_PREVIOUS":
                        previousSong();
                        loadMedia(songsList.get(songIterator).getSongID());
                        break;
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        pause();
                        break;
                }
            } catch (Exception e) {

            }
        }
    }

    private void nextSong() {
        if (shuffle) {
            if(index < 0){
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
        } else {
            songIterator++;
        }
    }

    private void previousSong() {
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
}
