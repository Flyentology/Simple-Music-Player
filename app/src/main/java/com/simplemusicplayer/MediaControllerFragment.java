package com.simplemusicplayer;

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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MediaControllerFragment extends Fragment {

    private View v;
    private TextView songName, artistName;
    private ProgressBar playbackProgress;
    private TextView currentDuration, totalDuration;
    private ImageButton pauseButton;
    private ServiceReceiver serviceReceiver = new ServiceReceiver();

    public MediaControllerFragment() {

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

        songName = v.findViewById(R.id.songName);
        artistName = v.findViewById(R.id.nameOfArtist);
        totalDuration = v.findViewById(R.id.duration);
        currentDuration = v.findViewById(R.id.currentDuration);
        playbackProgress = v.findViewById(R.id.progressBar);

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

        SharedPreferences mSettings = getActivity().getSharedPreferences("TextView", Context.MODE_PRIVATE);
        songName.setText(mSettings.getString("songName", ""));
        artistName.setText(mSettings.getString("artistName", ""));
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
                    totalDuration.setText(calculateDuration(maxDuration));
                    int currentTime = intent.getIntExtra("TIME", 0);
                    playbackProgress.setProgress(currentTime);
                    currentDuration.setText(calculateDuration(currentTime));
                    break;
                case "ICON_PAUSE":
                    pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_start));
                    break;
                case "ICON_RESUME":
                    pauseButton.setImageDrawable(getActivity().getDrawable(R.drawable.ic_pause));
                    break;
                case "SONG_DATA":
                    String tempSongName = intent.getStringExtra("SONG_NAME");
                    String tempArtistName = intent.getStringExtra("ARTIST_NAME");
                    songName.setText(tempSongName);
                    artistName.setText(tempArtistName);
                    // Add song data to shared preferences
                    SharedPreferences mSettings = getActivity().getSharedPreferences("TextView", Context.MODE_PRIVATE);
                    SharedPreferences.Editor mEditor = mSettings.edit();
                    mEditor.putString("songName", tempSongName);
                    mEditor.putString("artistName", tempArtistName);
                    mEditor.apply();
                    break;
            }
        }
    }

    private String calculateDuration(int duration) {
        int currentDuration = duration / 1000;
        int seconds = currentDuration % 60;
        currentDuration /= 60;
        return currentDuration + ":" + String.format("%02d", seconds);
    }
}
