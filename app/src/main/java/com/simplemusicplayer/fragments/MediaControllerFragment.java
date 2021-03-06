package com.simplemusicplayer.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.devadvance.circularseekbar.CircularSeekBar;
import com.simplemusicplayer.PlaybackLogic;
import com.simplemusicplayer.R;
import com.simplemusicplayer.activities.MainActivity;
import com.simplemusicplayer.activities.PlaybackActivity;
import com.simplemusicplayer.services.MediaPlaybackService;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Fragment that appears in every activity and indicates informations about song, playback progress and gives the user controls.
 */
public class MediaControllerFragment extends Fragment {

    private View v;
    private TextView songName, artistName;
    private CircularSeekBar playbackProgress;
    private CircleImageView circleImageView;
    private ImageButton pauseButton;
    private ServiceReceiver serviceReceiver = new ServiceReceiver();
    private SharedPreferences mSettings;
    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;

    private static int mCurrentState;

    private MediaBrowserCompat mMediaBrowserCompat;
    private MediaControllerCompat mMediaControllerCompat;

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mMediaControllerCompat = new MediaControllerCompat(getActivity(), mMediaBrowserCompat.getSessionToken());
                MediaControllerCompat.setMediaController(getActivity(), mMediaControllerCompat);
                // check if there is metadata art possible to load
                if (mMediaControllerCompat != null && mMediaControllerCompat.getMetadata() != null) {
                    Glide.with(getActivity()).load(mMediaControllerCompat.getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ART)).apply(new RequestOptions().override(80, 80).
                            placeholder(R.drawable.ic_empty_cover)).into(circleImageView);
                }
            } catch (RemoteException e) {

            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceStace) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_mediacontroller, container, false);
        configureFragmentUI();
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        mSettings = getActivity().getSharedPreferences("SONG_DATA", Context.MODE_PRIVATE);

        mMediaBrowserCompat = new MediaBrowserCompat(getActivity(), new ComponentName(getActivity(), MediaPlaybackService.class),
                mMediaBrowserCompatConnectionCallback, getActivity().getIntent().getExtras());

        mMediaBrowserCompat.connect();
    }

    /**
     * Method used when inflating the view and initialising UI elements.
     * Also sets up onClickListeners for various elements.
     */
    private void configureFragmentUI() {
        ImageButton playNext = v.findViewById(R.id.playNext);
        ImageButton playPrevious = v.findViewById(R.id.playPrevious);
        pauseButton = v.findViewById(R.id.pause);
        pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_pause));
        Button startPlaybackActivity = v.findViewById(R.id.start_playback_activity);

        songName = v.findViewById(R.id.songName);
        artistName = v.findViewById(R.id.nameOfArtist);
        playbackProgress = v.findViewById(R.id.progressBar);
        playbackProgress.setPointerAlpha(100);
        playbackProgress.setIsTouchEnabled(false);
        circleImageView = v.findViewById(R.id.circleImageView);

        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().skipToNext();
            }
        });

        playPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().skipToPrevious();
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MediaPlaybackService.songToPlay == null) {
                    if (PlaybackLogic.getSongsList().size() >= 1) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("SONG_TO_PLAY", PlaybackLogic.getSongsList().get(PlaybackLogic.getSongIterator()));
                        MediaControllerCompat.getMediaController(getActivity()).getTransportControls().playFromMediaId(String.valueOf(PlaybackLogic.getSongsList()
                                        .get(PlaybackLogic.getSongIterator()).getSongID())
                                , bundle);
                        PlaybackLogic.getPreviouslyPlayed().add(PlaybackLogic.getSongsList().get(PlaybackLogic.getSongIterator()));
                    }
                } else {
                    if (mCurrentState == STATE_PAUSED) {
                        MediaControllerCompat.getMediaController(getActivity()).getTransportControls().play();
                        pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_pause));
                        mCurrentState = STATE_PLAYING;
                    } else {
                        MediaControllerCompat.getMediaController(getActivity()).getTransportControls().pause();
                        pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_start));
                        mCurrentState = STATE_PAUSED;
                    }
                }
            }
        });

        startPlaybackActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), PlaybackActivity.class));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("CURRENT_SONG");
        intentFilter.addAction("PROGRESS");
        intentFilter.addAction("ICON_PAUSE");
        intentFilter.addAction("ICON_RESUME");
        intentFilter.addAction("SONG_DATA");
        intentFilter.addAction("APPLY_COVER");
        getActivity().registerReceiver(serviceReceiver, intentFilter);

        songName.setText(mSettings.getString("SONG_NAME", ""));
        artistName.setText(mSettings.getString("ARTIST_NAME", ""));

        // change icon to paused when user see's fragment again
        if (mSettings.getBoolean("IS_PAUSED", false)) {
            pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_start));
            mCurrentState = STATE_PAUSED;
        } else {
            pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_pause));
            mCurrentState = STATE_PLAYING;
        }
        // retrieve song progress and set it again
        playbackProgress.setProgress(mSettings.getInt("CURRENT_POSITION", 0));
        playbackProgress.setMax(mSettings.getInt("TOTAL_DURATION", 0));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(serviceReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowserCompat.disconnect();
    }

    /**
     * Nested BroadcastReceiver to handle incoming data about currently playing song.
     */
    private class ServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case "PROGRESS":
                    //int maxDuration = intent.getIntExtra("TOTAL_DURATION", 0);
                    //playbackProgress.setMax(maxDuration);
                    int currentTime = intent.getIntExtra("TIME", 0);
                    playbackProgress.setProgress(currentTime);
                    break;
                case "ICON_PAUSE":
                    pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_start));
                    break;
                case "ICON_RESUME":
                    pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_pause));
                    break;
                case "SONG_DATA":
                    songName.setText(mSettings.getString("SONG_NAME", ""));
                    artistName.setText(mSettings.getString("ARTIST_NAME", ""));
                    playbackProgress.setMax(mSettings.getInt("TOTAL_DURATION", 0));
                    break;
                case "APPLY_COVER":
                    // check if there is metadata art possible to load
                    if (mMediaControllerCompat != null && mMediaControllerCompat.getMetadata() != null) {
                        Glide.with(getActivity()).load(mMediaControllerCompat.getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ART)).apply(new RequestOptions().override(80, 80).
                                placeholder(R.drawable.ic_empty_cover)).into(circleImageView);
                    }
                    break;
            }
        }
    }
}
