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
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.service.media.MediaBrowserService;
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

import com.simplemusicplayer.MediaStyleHelper;
import com.simplemusicplayer.PlaybackLogic;
import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Song;

import java.io.IOException;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener {

    private MediaPlayer mMediaPlayer;
    private AudioManager audioManager;
    private MediaSessionCompat mMediaSessionCompat;
    private PlaybackLogic playbackLogic;
    private Song songToPlay;

    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
        createMediaSession();
        playbackLogic = new PlaybackLogic();
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

    // receiver that listens for audio becoming noisy
    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
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

            if(songToPlay != null){
                mMediaSessionCompat.setActive(true);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                startForeground(1337, showPlayingNotification());
                mMediaPlayer.start();

                IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
                registerReceiver(mNoisyReceiver, filter);

                UpdateProgressBar updateProgressBar = new UpdateProgressBar(songToPlay.getSongName(), songToPlay.getArtistName(), songToPlay.getPath());
                updateProgressBar.start();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            Thread.dumpStack();
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                showPausedNotification();
                unregisterReceiver(mNoisyReceiver);
                stopForeground(false);
            }
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            mMediaPlayer.reset();
            Song song = playbackLogic.nextSong();
            Bundle bundle = new Bundle();
            bundle.putParcelable("SONG_TO_PLAY", song);
            onPlayFromMediaId(String.valueOf(song.getSongID()), bundle);
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();

            mMediaPlayer.reset();
            Song song = playbackLogic.previousSong();
            Bundle bundle = new Bundle();
            bundle.putParcelable("SONG_TO_PLAY", song);
            onPlayFromMediaId(String.valueOf(song.getSongID()), bundle);
            onPlay();
        }

        @Override
        public void onStop() {
            super.onStop();
            mMediaPlayer.stop();
            mMediaSessionCompat.setActive(false);
            audioManager.abandonAudioFocus(onAudioFocusChangeListener);
            stopForeground(true);
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

    private PlaybackStateCompat.Builder mStateBuilder;

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
        NotificationCompat.Builder builder = MediaStyleHelper.from(MediaPlaybackService.this, mMediaSessionCompat);
        if (builder == null) {
            return null;
        }
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));
        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0).setMediaSession(mMediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        return builder.build();
    }

    private void showPausedNotification() {
        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mMediaSessionCompat);
        if (builder == null) {
            return;
        }

        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));
        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0).setMediaSession(mMediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        NotificationManagerCompat.from(MediaPlaybackService.this).notify(1337, builder.build());
    }

    private void initMediaSessionMetadata(Song song) {
        // TODO: fix metadata for lockscreen
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        //Notification icon in card
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, song.getSongName());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, song.getArtistName());
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
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
            mEditor.apply();
            Intent songData = new Intent("SONG_DATA");
            sendBroadcast(songData);

            while (mMediaPlayer.isPlaying()) {
                int songPosition = mMediaPlayer.getCurrentPosition();
                // save song progress and total duration in shared preferences
                mEditor.putInt("CURRENT_POSITION", songPosition);
                mEditor.putInt("TOTAL_DURATION", mMediaPlayer.getDuration());
                // send current progress and total duration to listening fragment/activity
                Intent intent = new Intent("PROGRESS");
                intent.putExtra("TIME", songPosition);
                intent.putExtra("TOTAL_DURATION", mMediaPlayer.getDuration());
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

}
