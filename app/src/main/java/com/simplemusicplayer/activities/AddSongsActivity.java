package com.simplemusicplayer.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;

import com.simplemusicplayer.SongUtils;
import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Song;
import com.simplemusicplayer.adapters.SongListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**Activity that allows user to choose and add songs to currently edited playlist.*/
public class AddSongsActivity extends AppCompatActivity {

    List<Song> songsList = new ArrayList<>();
    SongListAdapter songListAdapter;
    List<Song> adapterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_songs);

        ListView songList = findViewById(R.id.addSongList);
        adapterList = SongUtils.fillSongList(this, "COLLATE NOCASE ASC");
        songListAdapter = new SongListAdapter(this, adapterList);
        songList.setAdapter(songListAdapter);

        ImageButton sortSongList = findViewById(R.id.add_songs_sort_button);
        sortSongList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AddSongsActivity.this);
                builder.setTitle(R.string.sort_songs)
                        .setItems(R.array.sort_type, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                adapterList.clear();
                                switch (which) {
                                    case 0:
                                        if (SongUtils.sortByTitle) {
                                            SongUtils.alreadySorted = false;
                                            SongUtils.sortByTitle = false;
                                        }
                                        if (SongUtils.alreadySorted) {
                                            adapterList.addAll(SongUtils.fillSongList(AddSongsActivity.this, "COLLATE NOCASE DESC"));
                                        } else {
                                            adapterList.addAll(SongUtils.fillSongList(AddSongsActivity.this, "COLLATE NOCASE ASC"));
                                        }
                                        SongUtils.alreadySorted = !SongUtils.alreadySorted;
                                        break;
                                    case 1:
                                        if (!SongUtils.sortByTitle) {
                                            SongUtils.alreadySorted = false;
                                            SongUtils.sortByTitle = true;
                                        }
                                        if (SongUtils.alreadySorted) {
                                            adapterList.addAll(SongUtils.fillSongList(AddSongsActivity.this, "COLLATE NOCASE DESC"));
                                        } else {
                                            adapterList.addAll(SongUtils.fillSongList(AddSongsActivity.this, "COLLATE NOCASE ASC"));
                                        }
                                        SongUtils.alreadySorted = !SongUtils.alreadySorted;
                                        break;
                                }
                                songListAdapter.notifyDataSetChanged();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    /**Method that is called when user presses OK button.
     * Adds all selected songs to ArrayList and sends it back to {@link PlaylistViewActivity}.*/
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

