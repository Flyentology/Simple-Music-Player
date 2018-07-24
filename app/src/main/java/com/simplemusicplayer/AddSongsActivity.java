package com.simplemusicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

public class AddSongsActivity extends AppCompatActivity {

    ArrayList<Song> songsList = new ArrayList<>();
    SongListAdapter songListAdapter;
    ArrayList<Song> adapterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_songs);


        ListView songList = findViewById(R.id.addSongList);
        adapterList = FillSongList.fillSongList(this, 0);
        songListAdapter = new SongListAdapter(this, adapterList);

        songList.setAdapter(songListAdapter);
    }

    public void onButtonClick(View view) {
        for (int i = 0; i < adapterList.size(); i++) {
            if (adapterList.get(i).isSelected()) {
                songsList.add(adapterList.get(i));
            }
        }
        //send added songs back to the playlistViewActivity
        Intent resultIntent = new Intent();
        resultIntent.putParcelableArrayListExtra("SONGS_TO_ADD", songsList);
        setResult(PlaylistViewActivity.RESULT_OK, resultIntent);
        finish();
    }
}

