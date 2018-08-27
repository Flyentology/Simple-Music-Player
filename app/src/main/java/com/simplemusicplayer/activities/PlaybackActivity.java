package com.simplemusicplayer.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.simplemusicplayer.PlaybackLogic;
import com.simplemusicplayer.R;
import com.simplemusicplayer.services.MediaPlaybackService;

import java.util.Locale;

/**
 * Activity that shows song data, cover art, current progress and playback controls.
 */
public class PlaybackActivity extends AppCompatActivity {

    private TextView songTitle, totalDuration, currentDuration;
    private ImageView coverArt;
    private ImageButton pauseButton, playbackType;
    private SeekBar songProgress;
    private ServiceReceiver serviceReceiver;
    private SharedPreferences mSettings;
    private static int playbackIcon = 0;
    private static Bitmap bitmap;

    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;

    private static int mCurrentState;

    private static MediaBrowserCompat mMediaBrowserCompat;
    private static MediaControllerCompat mMediaControllerCompat;

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mMediaControllerCompat = new MediaControllerCompat(PlaybackActivity.this, mMediaBrowserCompat.getSessionToken());
                MediaControllerCompat.setMediaController(PlaybackActivity.this, mMediaControllerCompat);
                // check if there is metadata art possible to load
                if (mMediaControllerCompat != null && mMediaControllerCompat.getMetadata() != null) {
                    Glide.with(PlaybackActivity.this).load(mMediaControllerCompat.getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ART))
                            .error(Glide.with(PlaybackActivity.this).load(R.drawable.blank_cd)).into(coverArt);
                }
            } catch (RemoteException e) {

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        coverArt = findViewById(R.id.viewCover_playback);
        songTitle = findViewById(R.id.songName_activity_playback);
        songTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        songTitle.setSingleLine(true);
        songTitle.setSelected(true);
        ImageButton finishActivity = findViewById(R.id.finish_playback_activity);
        ImageButton playNext = findViewById(R.id.playNext_playback_activity);
        ImageButton playPrevious = findViewById(R.id.playPrevious_playback_activity);
        pauseButton = findViewById(R.id.pause_playback_activity);
        playbackType = findViewById(R.id.playback_type);
        songProgress = findViewById(R.id.seekBar_playback_activity);
        totalDuration = findViewById(R.id.totalDuration_playback_activity);
        currentDuration = findViewById(R.id.currentDuration_playback_activity);

        mSettings = getSharedPreferences("SONG_DATA", Context.MODE_PRIVATE);
        serviceReceiver = new ServiceReceiver();

        mMediaBrowserCompat = new MediaBrowserCompat(PlaybackActivity.this, new ComponentName(PlaybackActivity.this, MediaPlaybackService.class),
                mMediaBrowserCompatConnectionCallback, PlaybackActivity.this.getIntent().getExtras());
        mMediaBrowserCompat.connect();

        finishActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(PlaybackActivity.this).getTransportControls().skipToNext();
            }
        });

        playPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(PlaybackActivity.this).getTransportControls().skipToPrevious();
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MediaPlaybackService.songToPlay == null) {
                    if (PlaybackLogic.getSongsList().size() >= 1) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("SONG_TO_PLAY", PlaybackLogic.getSongsList().get(PlaybackLogic.getSongIterator()));
                        MediaControllerCompat.getMediaController(PlaybackActivity.this).getTransportControls().playFromMediaId(String.valueOf(PlaybackLogic.getSongsList()
                                        .get(PlaybackLogic.getSongIterator()).getSongID())
                                , bundle);
                        PlaybackLogic.getPreviouslyPlayed().add(PlaybackLogic.getSongsList().get(PlaybackLogic.getSongIterator()));
                    }
                } else {
                    if (mCurrentState == STATE_PAUSED) {
                        MediaControllerCompat.getMediaController(PlaybackActivity.this).getTransportControls().play();
                        mCurrentState = STATE_PLAYING;
                    } else {
                        if (MediaControllerCompat.getMediaController(PlaybackActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                            MediaControllerCompat.getMediaController(PlaybackActivity.this).getTransportControls().pause();
                        }
                        mCurrentState = STATE_PAUSED;
                    }
                }
            }
        });

        playbackType.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View view) {
                setPlaybackType(playbackIcon);
            }
        });

        songProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()

        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser) {
                    MediaControllerCompat.getMediaController(PlaybackActivity.this).getTransportControls().seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("CURRENT_SONG");
        intentFilter.addAction("PROGRESS");
        intentFilter.addAction("ICON_PAUSE");
        intentFilter.addAction("ICON_RESUME");
        intentFilter.addAction("SONG_DATA");
        intentFilter.addAction("APPLY_COVER");
        registerReceiver(serviceReceiver, intentFilter);
        songTitle.setText(mSettings.getString("SONG_NAME", "") + " - " + mSettings.getString("ARTIST_NAME", ""));

        SharedPreferences playbackSettings = getSharedPreferences("PLAYBACK_TYPE", Context.MODE_PRIVATE);
        setPlaybackType(playbackSettings.getInt("PLAYBACK_ICON", 0));

        // change icon to paused when user see's playback activity again
        if (mSettings.getBoolean("IS_PAUSED", false)) {
            pauseButton.setImageDrawable(getDrawable(R.drawable.ic_start_dark));
            mCurrentState = STATE_PAUSED;
        } else {
            pauseButton.setImageDrawable(getDrawable(R.drawable.ic_pause_dark));
            mCurrentState = STATE_PLAYING;
        }
        // retrieve song progress and set it again
        songProgress.setProgress(mSettings.getInt("CURRENT_POSITION", 0));
        songProgress.setMax(mSettings.getInt("TOTAL_DURATION", 0));
        currentDuration.setText(calculateDuration(mSettings.getInt("CURRENT_POSITION", 0)));
        totalDuration.setText(calculateDuration(mSettings.getInt("TOTAL_DURATION", 0)));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(serviceReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowserCompat.disconnect();
    }

    private class ServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();

            switch (intentAction) {
                case "PROGRESS":
                    int currentTime = intent.getIntExtra("TIME", 0);
                    songProgress.setProgress(currentTime);
                    currentDuration.setText(calculateDuration(currentTime));
                    break;
                case "ICON_PAUSE":
                    pauseButton.setImageDrawable(getDrawable(R.drawable.ic_start_dark));
                    break;
                case "ICON_RESUME":
                    pauseButton.setImageDrawable(getDrawable(R.drawable.ic_pause_dark));
                    break;
                case "SONG_DATA":
                    songTitle.setText(mSettings.getString("SONG_NAME", "") + " - " + mSettings.getString("ARTIST_NAME", ""));
                    totalDuration.setText(calculateDuration(mSettings.getInt("TOTAL_DURATION", 0)));
                    songProgress.setMax(mSettings.getInt("TOTAL_DURATION", 0));
                    break;
                case "APPLY_COVER":
                    // check if there is metadata art possible to load
                    if (mMediaControllerCompat != null && mMediaControllerCompat.getMetadata() != null) {
                        Glide.with(PlaybackActivity.this).load(mMediaControllerCompat.getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ART))
                                .error(Glide.with(PlaybackActivity.this).load(R.drawable.blank_cd)).into(coverArt);
                    }
                    break;
            }
        }
    }

    /**
     * Method that calculated duration from milliseconds to seconds and minutes.
     *
     * @param duration value in milliseconds.
     */
    private String calculateDuration(int duration) {
        int currentDuration = duration / 1000;
        int seconds = currentDuration % 60;
        currentDuration /= 60;
        return String.format(Locale.US, "%d:%02d", currentDuration, seconds);
    }

    /**
     * Method that determines current playback type and playback icon. It communicates with
     * {@link PlaybackLogic} to set appropriate values. Then saves it to shared preferences.
     *
     * @param playbackIconType int that determines playback type.
     */
    private void setPlaybackType(int playbackIconType) {
        switch (playbackIconType) {
            case 0:
                playbackType.setImageDrawable(getResources().getDrawable(R.drawable.ic_shuffle));
                PlaybackLogic.setPlayAgain(false);
                PlaybackLogic.setShuffle(true);
                PlaybackLogic.removeHistory();
                playbackIcon = 1;
                break;
            case 1:
                playbackType.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_normal));
                PlaybackLogic.setShuffle(false);
                PlaybackLogic.removeHistory();
                playbackIcon = 2;
                break;
            case 2:
                playbackType.setImageDrawable(getResources().getDrawable(R.drawable.ic_repeat_more));
                PlaybackLogic.setPlayAgain(true);
                playbackIcon = 0;
                break;
        }
        SharedPreferences mPlaybackSettings = getSharedPreferences("PLAYBACK_TYPE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPlaybackSettings.edit();
        editor.putInt("PLAYBACK_ICON", playbackIconType);
        editor.apply();
    }
}
