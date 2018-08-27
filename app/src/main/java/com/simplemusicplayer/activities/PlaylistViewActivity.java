package com.simplemusicplayer.activities;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.simplemusicplayer.PlaybackLogic;
import com.simplemusicplayer.fragments.MediaControllerFragment;
import com.simplemusicplayer.adapters.PlaylistViewAdapter;
import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Playlist;
import com.simplemusicplayer.models.Song;
import com.simplemusicplayer.services.MediaPlaybackService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Activity that shows selected playlist content using {@link PlaylistViewAdapter}.
 */
public class PlaylistViewActivity extends AppCompatActivity {

    private ArrayList<Song> playlistSongs;
    private final int RECEIVE_SONGS = 1;
    private PlaylistViewAdapter playlistAdapter;
    private ImageView coverArt;
    private RelativeLayout relativeLayout;
    private int playlistIndex;
    private ArrayList<Playlist> playlists;
    private static Handler mHandler;
    private static int loadIndex = 0;

    private static MediaBrowserCompat mMediaBrowserCompat;
    private static MediaControllerCompat mMediaControllerCompat;

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mMediaControllerCompat = new MediaControllerCompat(PlaylistViewActivity.this, mMediaBrowserCompat.getSessionToken());
                MediaControllerCompat.setMediaController(PlaylistViewActivity.this, mMediaControllerCompat);
            } catch (RemoteException e) {

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_view);

        relativeLayout = findViewById(R.id.relativeLayout);
        coverArt = findViewById(R.id.viewCover);
        //add songs that already are in the playlist
        Intent intent = getIntent();

        playlistIndex = intent.getIntExtra("PLAYLIST_INDEX", 0);
        playlists = intent.getParcelableArrayListExtra("PLAYLISTS");
        playlistSongs = playlists.get(playlistIndex).getPlaylistSongs();

        ListView playlistView = findViewById(R.id.playlistSongs);
        playlistAdapter = new PlaylistViewAdapter(this, playlists.get(playlistIndex).getPlaylistSongs(), playlists);
        playlistView.setAdapter(playlistAdapter);

        Toolbar toolbar = findViewById(R.id.playlist_view_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(PlaylistViewActivity.RESULT_OK);
                finish();
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
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
                if (!MediaPlaybackService.isRunning) {
                    startService(new Intent(PlaylistViewActivity.this, MediaPlaybackService.class));
                }
                PlaybackLogic.setSongsList(playlistSongs);
                PlaybackLogic.setSongIterator(i);
                PlaybackLogic.removeHistory();
                PlaybackLogic.setPlayingPlaylist(true);
                Bundle bundle = new Bundle();
                bundle.putParcelable("SONG_TO_PLAY", playlistSongs.get(i));
                MediaControllerCompat.getMediaController(PlaylistViewActivity.this).getTransportControls().playFromMediaId(String.valueOf(playlistSongs.get(i).getSongID())
                        , bundle);
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

        mHandler = new IncomingHandler(this);
        loadImage(0, mHandler);
    }


    @Override
    public void onStart() {
        super.onStart();
        mMediaBrowserCompat = new MediaBrowserCompat(PlaylistViewActivity.this, new ComponentName(PlaylistViewActivity.this, MediaPlaybackService.class),
                mMediaBrowserCompatConnectionCallback, PlaylistViewActivity.this.getIntent().getExtras());
        mMediaBrowserCompat.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load cover for playlist if it's available
    }

    @Override
    public void onPause() {
        super.onPause();
        PlaylistActivity.writeJSON(playlists, PlaylistViewActivity.this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowserCompat.disconnect();
    }

    /**
     * Get songs from {@link AddSongsActivity}, convert them to linkedHashSet.
     * Check if they don't already exist in current playlist.
     * Add all missing songs and trigger thread that loads cover art.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RECEIVE_SONGS: {
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<Song> temporaryList = data.getParcelableArrayListExtra("SONGS_TO_ADD");
                    if (temporaryList.size() > 0) {
                        ArrayList<Song> duplicates = new ArrayList<>();
                        Set<Song> songsToAdd = new LinkedHashSet<>(temporaryList);
                        for (Song song : playlistSongs) {
                            if (songsToAdd.contains(song)) {
                                duplicates.add(song);
                            }
                        }
                        songsToAdd.removeAll(duplicates);
                        playlists.get(playlistIndex).getPlaylistSongs().addAll(songsToAdd);
                        // save playlists after adding song to one of them
                        PlaylistActivity.writeJSON(playlists, PlaylistViewActivity.this);
                        //Load cover
                        loadImage(0, mHandler);
                        playlistAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    /**
     * Method assigns bitmap to image view or empty cover.
     * Creates duplicate and applies RenderScript blur effect that's used for background
     */
    private void setBackgroundImage(Drawable resource) {

        BitmapDrawable cover = (BitmapDrawable) resource;
        Bitmap toClone = cover.getBitmap();
        Bitmap clone = toClone.copy(toClone.getConfig(), true);// Create cropped clone of existing bitmap
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

    private void loadImage(final int index, final Handler mHandler) {
        if (index < playlistSongs.size()) {
            Glide.with(PlaylistViewActivity.this).load(playlistSongs.get(index).getPath()).apply(new RequestOptions().override(120, 120)).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    mHandler.obtainMessage(0).sendToTarget();
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    loadIndex = 0;
                    setBackgroundImage(resource);
                    playlists.get(playlistIndex).setPathToCover(playlistSongs.get(index).getPath());
                    return false;
                }
            }).into(coverArt);
        } else {
            playlists.get(playlistIndex).setPathToCover(null);
            Glide.with(PlaylistViewActivity.this).load(R.drawable.ic_empty_cover).into(coverArt);
        }
    }

    static class IncomingHandler extends Handler {

        private final WeakReference<PlaylistViewActivity> playlistViewActivity;

        public IncomingHandler(PlaylistViewActivity playlistViewActivity) {
            this.playlistViewActivity = new WeakReference<>(playlistViewActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaylistViewActivity activity = playlistViewActivity.get();
            if (msg.what == 0) {
                loadIndex++;
                Log.d("ddd", " " + loadIndex);
                activity.loadImage(loadIndex, mHandler);
            } else {
                loadIndex = 0;
            }
        }
    }
}