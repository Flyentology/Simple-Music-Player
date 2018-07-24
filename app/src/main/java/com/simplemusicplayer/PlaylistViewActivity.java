package com.simplemusicplayer;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class PlaylistViewActivity extends AppCompatActivity {

    private ArrayList<Song> playlistSongs = new ArrayList<>();
    private final int RECEIVE_SONGS = 1;
    private SongAdapter playlistAdapter;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_view);

        final ImageView coverArt = findViewById(R.id.viewCover);
        //add songs that already are in the playlist
        Intent intent = getIntent();
        ArrayList<Song> list = intent.getParcelableArrayListExtra("Playlist Content");
        playlistSongs.addAll(list);

        ListView playlistView = findViewById(R.id.playlistSongs);
        playlistAdapter = new SongAdapter(this, this, playlistSongs);
        playlistView.setAdapter(playlistAdapter);


        TextView playlistTitle = findViewById(R.id.playlistTitle);
        playlistTitle.setText(intent.getStringExtra("PLAYLIST_NAME"));

        ImageButton addSongs = findViewById(R.id.addSongs);
        addSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(PlaylistViewActivity.this, AddSongsActivity.class), RECEIVE_SONGS);
            }
        });

        ImageButton navigateBack = findViewById(R.id.finishActivity);
        navigateBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultIntent = new Intent();
                resultIntent.putParcelableArrayListExtra("CURRENT_PLAYLIST", playlistSongs);
                setResult(PlaylistViewActivity.RESULT_OK, resultIntent);
                finish();
            }
        });

        ImageButton deletePlaylist = findViewById(R.id.ic_delete);
        deletePlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistViewActivity.this);

                builder.setTitle(R.string.delete_playlist)
                        .setMessage(R.string.delete_playlist_message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Intent resultIntent = new Intent();
                                setResult(PlaylistViewActivity.RESULT_CANCELED, resultIntent);
                                finish();
                            }
                        });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        mHandler = new Handler() {

            //wait for messages from each thread and refresh list
            public void handleMessage(android.os.Message msg) {
                Log.d("ddd", "imhere222");
                try {
                    coverArt.setImageBitmap(playlistSongs.get(msg.what).getCoverArt());
                } catch (Exception e) {

                }
            }
        };

        if (playlistSongs.size() > 0) {
            LoadCovers loadCovers = new LoadCovers(playlistSongs, mHandler, 0, playlistSongs.size(), 130, 130, false);
            loadCovers.start();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RECEIVE_SONGS: {
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<Song> temporaryList = data.getParcelableArrayListExtra("SONGS_TO_ADD");
                    playlistSongs.addAll(temporaryList);
                    //updateCovers.loadImage();
                    LoadCovers loadCovers = new LoadCovers(playlistSongs, mHandler, 0, playlistSongs.size(), 130, 130, false);
                    loadCovers.start();
                    playlistAdapter.notifyDataSetChanged();
                }
            }
        }
    }

}
