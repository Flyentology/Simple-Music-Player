package com.simplemusicplayer.activities;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.simplemusicplayer.fragments.MediaControllerFragment;
import com.simplemusicplayer.adapters.PlaylistViewAdapter;
import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Song;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class LastPlayedActivity extends AppCompatActivity {

    private ArrayList<Song> songsList = new ArrayList<>();
    private ReceiveSong receiveSong = new ReceiveSong();
    private PlaylistViewAdapter lastPlayedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last_played);

        Toolbar toolbar = findViewById(R.id.lastPlayed_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.last_played);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // add fragment with music player
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        MediaControllerFragment mediaControllerFragment = new MediaControllerFragment();
        transaction.add(R.id.lastPlayed_container, mediaControllerFragment);
        transaction.commit();

        //setting up the adapter
        ListView listView = findViewById(R.id.lastPlayed_songsList);
        lastPlayedAdapter = new PlaylistViewAdapter(this, songsList);
        listView.setAdapter(lastPlayedAdapter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister receiver
        unregisterReceiver(receiveSong);
    }

    @Override
    protected void onResume() {
        super.onResume();

        songsList.addAll(readJSON());
        lastPlayedAdapter.notifyDataSetChanged();
        //register receiver
        IntentFilter intentFilter = new IntentFilter("LAST_PLAYED_SONG");
        registerReceiver(receiveSong, intentFilter);
    }

    class ReceiveSong extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("LAST_PLAYED_SONG")) {
                songsList.addAll(readJSON());
                lastPlayedAdapter.notifyDataSetChanged();
            }

        }
    }

    /*
     * Method used to read and convert saved arraylist from Json
     */
    public ArrayList<Song> readJSON() {
        Gson mGson = new Gson();
        SharedPreferences mSettings = getSharedPreferences("LAST_PLAYED_SONGS", Context.MODE_PRIVATE);
        Type collectionType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        String loadValue = mSettings.getString("LAST_PLAYED_LIST", null);
        songsList.clear();
        return mGson.fromJson(loadValue, collectionType);
    }
}
