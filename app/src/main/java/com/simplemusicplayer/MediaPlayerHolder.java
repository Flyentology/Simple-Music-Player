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
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class MediaPlayerHolder implements MediaPlayer.OnCompletionListener, MediaController.MediaPlayerControl {


    private final Context mContext;
    private List<Song> songsList;
    private MediaPlayer mediaPlayer;
    private int songIterator = 0;
    private MediaController mediaController;
    private boolean playedOnce, shuffle = false;
    private List<Integer> previouslyPlayed;
    private ListIterator previousSong;
    private MainActivity mainActivity;

    public MediaPlayerHolder(Context mContext, MainActivity mainActivity) {
        this.mContext = mContext.getApplicationContext();
        this.songsList = new ArrayList<>();
        this.previouslyPlayed = new ArrayList<>();
        this.previousSong = previouslyPlayed.listIterator();
        this.mediaPlayer = new MediaPlayer();
        this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
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

    public MediaController getMediaController() {
        return mediaController;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public void setMediaController() {
        //mediaController = new MediaController(mContext);
        mediaController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //previouslyPlayed.add(songIterator);
                nextSong();
                loadMedia();

            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousSong();
                loadMedia();
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
    public void loadMedia() {
        reset();
        Uri uri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songsList.get(songIterator).getSongID());
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
        mainActivity.createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), "Pause");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //onCompletion is called on the beginning, condition won't allow it to play until someone choose song
        if (playedOnce) {
            nextSong();
            loadMedia();
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
                        loadMedia();
                        break;
                    case "PAUSE":
                        if (isPlaying()) {
                            pause();
                            mainActivity.createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), "Resume");
                        } else {
                            start();
                            mainActivity.createNotification(songsList.get(songIterator).getArtistName() + " " + songsList.get(songIterator).getSongName(), "Pause");
                        }
                        break;
                    case "PLAY_PREVIOUS":
                        previousSong();
                        loadMedia();
                        break;
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        pause();
                        break;
                }
            } catch (Exception e) {

            }
        }
    }

    //TODO: Fix repeating of last eleement of list by iterator
    private void nextSong() {
        if (shuffle) {
            if (previousSong.hasNext()) {
                int index = previousSong.nextIndex();
                songIterator = previouslyPlayed.get(index);
                previousSong.next();
            } else {
                Random r = new Random();
                songIterator = r.nextInt(songsList.size());
                previouslyPlayed.add(songIterator);
                previousSong = previouslyPlayed.listIterator(previouslyPlayed.size());
            }
        } else {
            songIterator++;
        }
    }

    private void previousSong() {
        if (shuffle) {
            if (previousSong.hasPrevious()) {
                int index = previousSong.previousIndex();
                songIterator = previouslyPlayed.get(index);
                previousSong.previous();
            } else {
                Random r = new Random();
                songIterator = r.nextInt(songsList.size());
                previouslyPlayed.add(0, songIterator);
                previousSong = previouslyPlayed.listIterator(0);
            }
        } else if (songIterator >= 1) {
            songIterator--;
        } else {
            songIterator = songsList.size() - 1;
        }
    }

    public void removeHistory(boolean isShuffled) {
        if (!isShuffled) {
            previouslyPlayed.clear();
            previousSong = previouslyPlayed.listIterator(0);
        }
    }
}
