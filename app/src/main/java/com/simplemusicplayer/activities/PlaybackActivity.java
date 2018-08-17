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

import com.simplemusicplayer.PlaybackLogic;
import com.simplemusicplayer.R;
import com.simplemusicplayer.services.MediaPlaybackService;

import java.util.Locale;

public class PlaybackActivity extends AppCompatActivity {

    private TextView songTitle, totalDuration, currentDuration;
    private ImageView coverArt;
    private ImageButton pauseButton, playbackType;
    private SeekBar songProgress;
    private ServiceReceiver serviceReceiver;
    private SharedPreferences mSettings;
    private static int playbackIcon = 0;

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
            } catch (RemoteException e) {

            }
        }
    };

    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if (state == null) {
                return;
            }

            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    mCurrentState = STATE_PLAYING;
                    pauseButton.setImageDrawable(getDrawable(R.drawable.ic_pause));
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    mCurrentState = STATE_PAUSED;
                    pauseButton.setImageDrawable(getDrawable(R.drawable.ic_start));
                    break;
                }
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

        mMediaBrowserCompat = new MediaBrowserCompat(PlaybackActivity.this, new ComponentName(PlaybackActivity.this, MediaPlaybackService.class),
                mMediaBrowserCompatConnectionCallback, PlaybackActivity.this.getIntent().getExtras());

        mMediaBrowserCompat.connect();

        mSettings = getSharedPreferences("SONG_DATA", Context.MODE_PRIVATE);
        serviceReceiver = new ServiceReceiver();

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
        });

        playbackType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setPlaybackType(playbackIcon);
            }
        });

        songProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("CURRENT_SONG");
        intentFilter.addAction("PROGRESS");
        intentFilter.addAction("ICON_PAUSE");
        intentFilter.addAction("ICON_RESUME");
        intentFilter.addAction("SONG_DATA");
        registerReceiver(serviceReceiver, intentFilter);
        songTitle.setText(mSettings.getString("SONG_NAME", "") + " - " + mSettings.getString("ARTIST_NAME", ""));
        String path = mSettings.getString("COVER_PATH", null);

        if (path != null) {
            new LoadCover().execute(path);
        }
        SharedPreferences playbackSettings = getSharedPreferences("PLAYBACK_TYPE", Context.MODE_PRIVATE);
        setPlaybackType(playbackSettings.getInt("PLAYBACK_ICON", 0));

        // change icon to paused when user see's playback activity again
        if (mSettings.getBoolean("IS_PAUSED", false)) {
            pauseButton.setImageDrawable(getDrawable(R.drawable.ic_start_dark));
        } else {
            pauseButton.setImageDrawable(getDrawable(R.drawable.ic_pause_dark));
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

    private class LoadCover extends AsyncTask<String, Integer, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            String path = strings[0];
            MediaMetadataRetriever metaRetreiver = new MediaMetadataRetriever();
            metaRetreiver.setDataSource(path);
            byte[] art = metaRetreiver.getEmbeddedPicture();
            if (art != null) {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inJustDecodeBounds = true; //just check size of image
                BitmapFactory.decodeByteArray(art, 0, art.length, opt);

                // assign values of image
                int imageHeight = opt.outHeight;
                int imageWidth = opt.outWidth;

                //condition to determine max inSample size
                if (imageHeight > 310 || imageWidth > 310) {
                    final int halfHeight = imageHeight / 2;
                    final int halfWidth = imageWidth / 2;
                    int inSampleSize = 1;
                    while ((halfHeight / inSampleSize) >= 310
                            && (halfWidth / inSampleSize) >= 310) {
                        inSampleSize *= 2;
                    }
                    opt.inSampleSize = inSampleSize;
                }
                opt.inJustDecodeBounds = false;
                return BitmapFactory.decodeByteArray(art, 0, art.length, opt);
            } else {
                return BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.drawable.ic_empty_cover);
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            coverArt.setImageBitmap(bitmap);
        }
    }

    private class ServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();

            switch (intentAction) {
                case "PROGRESS":
                    int maxDuration = intent.getIntExtra("TOTAL_DURATION", 0);
                    songProgress.setMax(maxDuration);
                    totalDuration.setText(calculateDuration(maxDuration));
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
                    String path = mSettings.getString("COVER_PATH", null);
                    if (path != null) {
                        new LoadCover().execute(path);
                    }
                    break;
            }
        }
    }

    private String calculateDuration(int duration) {
        int currentDuration = duration / 1000;
        int seconds = currentDuration % 60;
        currentDuration /= 60;
        return String.format(Locale.US, "%d:%02d", currentDuration, seconds);
    }

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
