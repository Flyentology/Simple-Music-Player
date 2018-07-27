package com.simplemusicplayer;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class PlaylistViewActivity extends AppCompatActivity {

    private ArrayList<Song> playlistSongs = new ArrayList<>();
    private final int RECEIVE_SONGS = 1;
    private PlaylistViewAdapter playlistAdapter;
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
        playlistAdapter = new PlaylistViewAdapter(this, playlistSongs);
        playlistView.setAdapter(playlistAdapter);

        Toolbar toolbar = findViewById(R.id.playlist_view_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(intent.getStringExtra("PLAYLIST_NAME"));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putParcelableArrayListExtra("CURRENT_PLAYLIST", playlistSongs);
                setResult(PlaylistViewActivity.RESULT_OK, resultIntent);
                finish();
            }
        });
        TextView playlistTitle = findViewById(R.id.playlistTitle);
        playlistTitle.setText(intent.getStringExtra("PLAYLIST_NAME"));

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MediaControllerFragment mediaControllerFragment = new MediaControllerFragment();
        fragmentTransaction.add(R.id.playlistViewActivity_container, mediaControllerFragment);
        fragmentTransaction.commit();

        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO: stop playback if user deletes playlist
                Intent sendPlaylist = new Intent("PLAY_PLAYLIST");
                sendPlaylist.putParcelableArrayListExtra("PLAYLIST", playlistSongs);
                sendPlaylist.putExtra("PLAYLIST_ITERATOR", i);
                sendBroadcast(sendPlaylist);
            }
        });

        ImageButton addSongs = findViewById(R.id.addSongs);
        addSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(PlaylistViewActivity.this, AddSongsActivity.class), RECEIVE_SONGS);
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
                                setResult(11);
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
