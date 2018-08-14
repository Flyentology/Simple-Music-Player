package com.simplemusicplayer.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.simplemusicplayer.R;
import com.simplemusicplayer.activities.PlaybackActivity;

public class MediaControllerFragment extends Fragment {

    private View v;
    private TextView songName, artistName;
    private CircularSeekBar playbackProgress;
    private TextView currentDuration, totalDuration;
    private ImageButton pauseButton;
    private ServiceReceiver serviceReceiver = new ServiceReceiver();
    private SharedPreferences mSettings;

    public MediaControllerFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = getActivity().getSharedPreferences("SONG_DATA", Context.MODE_PRIVATE);
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

        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent playNext = new Intent("PLAY_NEXT");
                getActivity().sendBroadcast(playNext);
            }
        });

        playPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent playPrevious = new Intent("PLAY_PREVIOUS");
                getActivity().sendBroadcast(playPrevious);
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pause = new Intent("PAUSE");
                getActivity().sendBroadcast(pause);
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
        getActivity().registerReceiver(serviceReceiver, intentFilter);

        songName.setText(mSettings.getString("SONG_NAME", ""));
        artistName.setText(mSettings.getString("ARTIST_NAME", ""));

        // change icon to paused when user see's fragment again
        if(mSettings.getBoolean("IS_PAUSED", false)){
            pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_start));
        } else {
            pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_pause));
        }
        // retrieve song progress and set it again
        playbackProgress.setProgress(mSettings.getInt("CURRENT_POSITION", 0));
        playbackProgress.setMax(mSettings.getInt("TOTAL_DURATION", 0));
        //currentDuration.setText(calculateDuration(mSettings.getInt("CURRENT_POSITION", 0)));
        //totalDuration.setText(calculateDuration(mSettings.getInt("TOTAL_DURATION", 0)));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(serviceReceiver);
    }

    private class ServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();

            switch (intentAction) {
                case "PROGRESS":
                    int maxDuration = intent.getIntExtra("TOTAL_DURATION", 0);
                    playbackProgress.setMax(maxDuration);
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
                    break;
            }
        }
    }

//    private String calculateDuration(int duration) {
//        int currentDuration = duration / 1000;
//        int seconds = currentDuration % 60;
//        currentDuration /= 60;
//        return currentDuration + ":" + String.format("%02d", seconds);
//    }
}
