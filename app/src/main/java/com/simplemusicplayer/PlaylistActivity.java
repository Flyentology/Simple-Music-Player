package com.simplemusicplayer;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {
    private ArrayList<Playlist> playlists = new ArrayList<>();
    private PlaylistAdapter playlistAdapter;
    private final int VIEW_PLAYLIST = 1;
    private int playlistPosition = 0;
    private Handler mHandler;
    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson mGson = gsonBuilder.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        final GridView playlistGrid = findViewById(R.id.playlist_grid);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.current_playlists);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeJSON(playlists);
                finish();
            }
        });

        // add fragment with music player
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        MediaControllerFragment mediaControllerFragment = new MediaControllerFragment();
        transaction.add(R.id.playlistActivity_container, mediaControllerFragment);
        transaction.commit();

        mHandler = new Handler() {
            //wait for messages from each thread and refresh list
            public void handleMessage(android.os.Message msg) {
                Log.d("ddd", "imhere");
                switch (msg.what) {
                    case 0:
                        playlistAdapter.notifyDataSetChanged();
                        break;
                }
            }
        };

        if (readJSON() != null) {
            playlists.addAll(readJSON());
            for (Playlist playlist : playlists) {
                LoadCovers loadCovers = new LoadCovers(mHandler, playlist, 130, 130);
                loadCovers.start();
            }
        }

        playlistAdapter = new PlaylistAdapter(this, playlists);
        playlistGrid.setAdapter(playlistAdapter);
        playlistGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playlistPosition = position;
                Intent viewPlaylist = new Intent(PlaylistActivity.this, PlaylistViewActivity.class);
                viewPlaylist.putParcelableArrayListExtra("Playlist Content", playlists.get(position).getPlaylistSongs());
                viewPlaylist.putExtra("PLAYLIST_NAME", playlists.get(position).getName());
                startActivityForResult(viewPlaylist, VIEW_PLAYLIST);
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.playlist_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.add:
                AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistActivity.this);
                //set up the input
                final EditText input = new EditText(this);
                //set up the input type
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setTitle(R.string.playlist_name)
                        .setMessage(R.string.playlist_message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String text = input.getText().toString();
                                playlists.add(new Playlist(text));
                                playlistAdapter.notifyDataSetChanged();
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
                return true;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case VIEW_PLAYLIST: {
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<Song> temporaryList = data.getParcelableArrayListExtra("CURRENT_PLAYLIST");
                    playlists.get(playlistPosition).getPlaylistSongs().clear();
                    playlists.get(playlistPosition).getPlaylistSongs().addAll(temporaryList);
                    if (playlists.get(playlistPosition).getPlaylistArt() == null) {
                        LoadCovers loadCovers = new LoadCovers(mHandler, playlists.get(playlistPosition), 130, 130);
                        loadCovers.start();
                    }
                    writeJSON(playlists);
                    playlistAdapter.notifyDataSetChanged();
                } else if (resultCode == 11) {
                    playlists.remove(playlistPosition);
                    writeJSON(playlists);
                    playlistAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    public void writeJSON(ArrayList<Playlist> playlists) {

        SharedPreferences mSettings = getSharedPreferences("Playlists", Context.MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mSettings.edit();
        Type type = new TypeToken<ArrayList<Playlist>>() {
        }.getType();
        try {
            String writeValue = mGson.toJson(playlists, type);
            mEditor.putString("Playlists", writeValue);
            mEditor.apply();
        } catch (Exception e) {
        }
    }

    public ArrayList<Playlist> readJSON() {

        SharedPreferences mSettings = getSharedPreferences("Playlists", Context.MODE_PRIVATE);
        Type collectionType = new TypeToken<ArrayList<Playlist>>() {
        }.getType();
        String loadValue = mSettings.getString("Playlists", null);
        return mGson.fromJson(loadValue, collectionType);
    }

}
