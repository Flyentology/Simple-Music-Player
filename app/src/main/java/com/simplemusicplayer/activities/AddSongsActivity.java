package com.simplemusicplayer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import com.simplemusicplayer.SongUtils;
import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Song;
import com.simplemusicplayer.adapters.SongListAdapter;

import java.util.ArrayList;
import java.util.List;

public class AddSongsActivity extends AppCompatActivity {

    List<Song> songsList = new ArrayList<>();
    SongListAdapter songListAdapter;
    List<Song> adapterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_songs);


        ListView songList = findViewById(R.id.addSongList);
        adapterList = SongUtils.fillSongList(this, 0);
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
        resultIntent.putParcelableArrayListExtra("SONGS_TO_ADD", (ArrayList<? extends Parcelable>) songsList);
        setResult(PlaylistViewActivity.RESULT_OK, resultIntent);
        finish();
    }
}

