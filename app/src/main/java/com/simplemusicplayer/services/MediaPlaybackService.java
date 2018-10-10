package com.simplemusicplayer.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.simplemusicplayer.MediaStyleHelper;
import com.simplemusicplayer.PlaybackLogic;
import com.simplemusicplayer.R;
import com.simplemusicplayer.SongUtils;
import com.simplemusicplayer.models.Song;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener {

    private MediaPlayer mMediaPlayer;
    private AudioManager audioManager;
    private MediaSessionCompat mMediaSessionCompat;
    private Bitmap metadataCover;
    public static Song songToPlay;
    public static boolean isRunning = true;
    private static boolean isLoaded = false;
    final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
        createMediaSession();
        isRunning = true;
        PlaybackLogic.readPreferences(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaSessionCompat.release();
        mMediaPlayer.release();
    }

    // listener to audio focus change
    AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                mMediaPlayer.pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (mMediaPlayer != null) {
                    if (!mMediaPlayer.isPlaying()) {
                        mMediaPlayer.start();
                    }
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(0.3f, 0.3f);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                mMediaPlayer.stop();
                audioManager.abandonAudioFocus(onAudioFocusChangeListener);
            }
        }
    };

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_GAIN) {
            // proceed with playing music
            mMediaSessionCompat.setActive(true);
        }
        return result == AudioManager.AUDIOFOCUS_GAIN;
    }

    // receiver that listens for audio becoming noisy or for worker manager broadcast
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("STOP")) {
                mCallback.onStop();
            } else {
                mCallback.onPause();
            }
        }
    };

    private void createMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);

        mMediaSessionCompat = new MediaSessionCompat(this, "ddd", mediaButtonReceiver, null);
        mMediaSessionCompat.setCallback(mCallback);
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        setSessionToken(mMediaSessionCompat.getSessionToken());
    }

    private MediaSessionCompat.Callback mCallback = new MediaSessionCompat.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay() {
            super.onPlay();
            if (!requestAudioFocus()) {
                return;
            }

            if (songToPlay != null) {
                mMediaSessionCompat.setActive(true);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

                if (isLoaded) {
                    startForeground(1337, showPlayingNotification());
                }
                mMediaPlayer.start();

                IntentFilter filter = new IntentFilter();
                filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
                filter.addAction("PAUSE_PLAYBACK");
                filter.addAction("STOP");
                registerReceiver(mReceiver, filter);

                UpdateProgressBar updateProgressBar = new UpdateProgressBar(songToPlay.getSongName(), songToPlay.getArtistName(), songToPlay.getPath());
                updateProgressBar.start();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                if (isLoaded) {
                    showPausedNotification();
                    stopForeground(false);
                }
            }
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            mMediaPlayer.reset();
            Song song = PlaybackLogic.nextSong();
            Bundle bundle = new Bundle();
            bundle.putParcelable("SONG_TO_PLAY", song);
            onPlayFromMediaId(String.valueOf(song.getSongID()), bundle);
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            mMediaPlayer.reset();
            Song song = PlaybackLogic.previousSong();
            Bundle bundle = new Bundle();
            bundle.putParcelable("SONG_TO_PLAY", song);
            onPlayFromMediaId(String.valueOf(song.getSongID()), bundle);
            onPlay();
        }

        @Override
        public void onStop() {
            super.onStop();
            mMediaPlayer.stop();
            unregisterReceiver(mReceiver);
            mMediaSessionCompat.setActive(false);
            audioManager.abandonAudioFocus(onAudioFocusChangeListener);
            isRunning = false;
            writeLastPlaylist(PlaybackLogic.getSongsList(), PlaybackLogic.getSongIterator(), getApplicationContext());
            stopForeground(true);
            stopSelf();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            mMediaPlayer.seekTo((int) pos);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            mMediaPlayer.reset();
            Uri uri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.valueOf(mediaId));
            try {
                mMediaPlayer.setDataSource(getApplicationContext(), uri);
                mMediaPlayer.prepare();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            //set class loader to prevent exception while unmarshalling
            extras.setClassLoader(getClass().getClassLoader());
            songToPlay = extras.getParcelable("SONG_TO_PLAY");
            initMediaSessionMetadata(songToPlay);
            PlaybackLogic.lastPlayedSongs(songToPlay, getApplicationContext());
            onPlay();
        }
    };

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(getString(R.string.app_name), null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setVolume(1.0f, 1.0f);
        mMediaPlayer.setOnCompletionListener(this);
    }

    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    private Notification showPlayingNotification() {

        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mMediaSessionCompat);

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
                .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
                .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mMediaSessionCompat.getSessionToken()))
                .build();

        return notification;
    }

    private void showPausedNotification() {
        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mMediaSessionCompat);
        if (builder == null) {
            return;
        }

        Intent intent = new Intent("STOP");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));
        builder.setDeleteIntent(pendingIntent);
        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2).setMediaSession(mMediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        NotificationManagerCompat.from(MediaPlaybackService.this).notify(1337, builder.build());
    }

    private void initMediaSessionMetadata(Song song) {

        if (metadataCover != null) {
            metadataCover.recycle();
        }

        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(song.getPathToFile());
        byte[] art = metaRetriever.getEmbeddedPicture();
        if (art != null) {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true; //check size of image and don't decode it
            BitmapFactory.decodeByteArray(art, 0, art.length, opt);
            // assign values of image
            int imageHeight = opt.outHeight;
            int imageWidth = opt.outWidth;

            //condition to determine max inSample size
            if (imageHeight > 200 || imageWidth > 200) {
                final int halfHeight = imageHeight / 2;
                final int halfWidth = imageWidth / 2;
                int inSampleSize = 1;
                while ((halfHeight / inSampleSize) >= 200
                        && (halfWidth / inSampleSize) >= 200) {
                    inSampleSize *= 2;
                }
                opt.inSampleSize = inSampleSize;
            }
            opt.inJustDecodeBounds = false;

            metadataCover = BitmapFactory.decodeByteArray(art, 0, art.length, opt);
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, metadataCover);
        } else {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, null);
        }
        putMetadata(song);
    }

    private void putMetadata(Song songToSave) {
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        //title and subtitle for notification
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, songToSave.getSongName());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, songToSave.getArtistName());
        //title and subtitle for lockscreen
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, songToSave.getSongName() + " - " + songToSave.getArtistName());

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
        isLoaded = true;
        sendBroadcast(new Intent("APPLY_COVER"));
        startForeground(1337, showPlayingNotification());
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (PlaybackLogic.getPlayAgain()) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("SONG_TO_PLAY", songToPlay);
            mCallback.onPlayFromMediaId(String.valueOf(songToPlay.getSongID())
                    , bundle);
        } else {
            mCallback.onSkipToNext();
        }
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
            mEditor.putInt("TOTAL_DURATION", mMediaPlayer.getDuration());
            mEditor.apply();
            Intent songData = new Intent("SONG_DATA");
            sendBroadcast(songData);
            int songPosition;

            while (mMediaPlayer.isPlaying()) {
                songPosition = mMediaPlayer.getCurrentPosition();
                // save song progress and total duration in shared preferences
                mEditor.putInt("CURRENT_POSITION", songPosition);
                //mEditor.putInt("TOTAL_DURATION", mMediaPlayer.getDuration());
                // send current progress and total duration to listening fragment/activity
                Intent intent = new Intent("PROGRESS");
                intent.putExtra("TIME", songPosition);
                //intent.putExtra("TOTAL_DURATION", mMediaPlayer.getDuration());
                sendBroadcast(intent);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }
            mEditor.putBoolean("IS_PAUSED", true);
            mEditor.apply();
            sendBroadcast(new Intent("ICON_PAUSE"));
        }
    }

    private void writeLastPlaylist(List<Song> playlistToSave, int songIterator, Context context) {
        Gson mGson = new Gson();
        SharedPreferences mSettings = context.getSharedPreferences("LAST_PLAYED_PLAYLIST", Context.MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mSettings.edit();
        Type type = new TypeToken<List<Song>>() {
        }.getType();
        try {
            String writeValue = mGson.toJson(playlistToSave, type);
            mEditor.putString("LAST_PLAYED_LIST", writeValue);
            mEditor.putInt("SONG_ITERATOR", songIterator);
            mEditor.apply();
        } catch (Exception e) {
        }
    }

}
