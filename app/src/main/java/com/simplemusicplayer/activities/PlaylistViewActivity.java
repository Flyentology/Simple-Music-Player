package com.simplemusicplayer.activities;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.simplemusicplayer.LoadCovers;
import com.simplemusicplayer.fragments.MediaControllerFragment;
import com.simplemusicplayer.adapters.PlaylistViewAdapter;
import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Song;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class PlaylistViewActivity extends AppCompatActivity {

    private ArrayList<Song> playlistSongs = new ArrayList<>();
    private final int RECEIVE_SONGS = 1;
    private PlaylistViewAdapter playlistAdapter;
    private ImageView coverArt;
    private Handler mHandler;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_view);

        relativeLayout = findViewById(R.id.relativeLayout);
        coverArt = findViewById(R.id.viewCover);
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
        getSupportActionBar().setTitle("");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putParcelableArrayListExtra("CURRENT_PLAYLIST", playlistSongs);
                setResult(PlaylistViewActivity.RESULT_OK, resultIntent);
                finish();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
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
                                sendBroadcast(new Intent("ABORT_PLAYBACK"));
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
            // Wait for message from thread
            public void handleMessage(android.os.Message msg) {
                    Bitmap cover = playlistSongs.get(msg.what).getCoverArt();
                    coverArt.setImageBitmap(cover); // Assign bitmap to imageView
                    Bitmap clone = Bitmap.createBitmap(cover, 20, 20, 100, 100); // Create cropped clone of existing bitmap
                    // Prepare RenderScript and script to blur cloned bitmap
                    RenderScript renderScript = RenderScript.create(PlaylistViewActivity.this);
                    final Allocation input = Allocation.createFromBitmap(renderScript, clone);
                    final Allocation output = Allocation.createTyped(renderScript, input.getType());
                    final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
                    script.setRadius(16f);
                    script.setInput(input);
                    script.forEach(output);
                    output.copyTo(clone);
                    // Create drawable and assign it as background
                    Drawable drawable = new BitmapDrawable(getResources(), clone);
                    relativeLayout.setBackground(drawable);
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load cover for playist if it's available
        if (playlistSongs.size() > 0) {
            LoadCovers loadCovers = new LoadCovers(playlistSongs, mHandler, 0, playlistSongs.size(), 200, 200, false);
            loadCovers.start();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RECEIVE_SONGS: {
                if (resultCode == Activity.RESULT_OK) {
                    /*
                     * Get songs from AddSongsActivity, convert them to linkedHashSet
                     * check if they don't already exist in current playlist
                     * add all missing songs
                     */
                    ArrayList<Song> temporaryList = data.getParcelableArrayListExtra("SONGS_TO_ADD");
                    ArrayList<Song> duplicates = new ArrayList<>();
                    Set<Song> songsToAdd = new LinkedHashSet<>(temporaryList);
                    for(Song song : playlistSongs){
                        if(songsToAdd.contains(song)){
                            duplicates.add(song);
                        }
                    }
                    songsToAdd.removeAll(duplicates);
                    playlistSongs.addAll(songsToAdd);
                    LoadCovers loadCovers = new LoadCovers(playlistSongs, mHandler, 0, playlistSongs.size(), 200, 200, false);
                    loadCovers.start();
                    playlistAdapter.notifyDataSetChanged();
                }
            }
        }
    }

}

//TODO: save playlist after adding songs