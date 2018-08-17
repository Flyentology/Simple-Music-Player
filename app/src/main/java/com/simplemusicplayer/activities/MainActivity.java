package com.simplemusicplayer.activities;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.simplemusicplayer.PlaybackLogic;
import com.simplemusicplayer.SongUtils;
import com.simplemusicplayer.LoadCovers;
import com.simplemusicplayer.fragments.MediaControllerFragment;
import com.simplemusicplayer.fragments.SettingsFragment;
import com.simplemusicplayer.services.MediaPlaybackService;
import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Song;
import com.simplemusicplayer.adapters.SongAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private final int MY_PERMISSIONS_REQUEST = 1;
    private ListView songsList;
    private boolean playShuffle = false;
    private boolean listChanged = false;
    private static int whichSortType = 0;
    public static AtomicBoolean stopThreads = new AtomicBoolean(false);
    private SongAdapter songAdapter;
    private List<Song> songsListView = new ArrayList<>();
    private List<Song> baseSongList = new ArrayList<>();
    private Handler mHandler;
    private int threadCount = 0;

    private static MediaBrowserCompat mMediaBrowserCompat;
    private static MediaControllerCompat mMediaControllerCompat;

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mMediaControllerCompat = new MediaControllerCompat(MainActivity.this, mMediaBrowserCompat.getSessionToken());
                MediaControllerCompat.setMediaController(MainActivity.this, mMediaControllerCompat);
            } catch (RemoteException e) {

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        startService(new Intent(this, MediaPlaybackService.class));

        mMediaBrowserCompat = new MediaBrowserCompat(MainActivity.this, new ComponentName(MainActivity.this, MediaPlaybackService.class),
                mMediaBrowserCompatConnectionCallback, MainActivity.this.getIntent().getExtras());

        mMediaBrowserCompat.connect();

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MediaControllerFragment mediaControllerFragment = new MediaControllerFragment();
        fragmentTransaction.add(R.id.mediaControllerContainer, mediaControllerFragment);
        fragmentTransaction.commit();

        mHandler = new Handler() {
            int count = 0;

            //wait for messages from each thread and refresh list
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case 0:
                        count++;
                        if (count == threadCount) {
                            songsListView.clear();
                            songsListView.addAll(baseSongList);
                            songAdapter.notifyDataSetChanged();
                            count = 0;
                            threadCount = 0;
                        }
                        break;
                }
            }
        };

        ListView mainMenu = findViewById(R.id.mainMenu);
        ArrayList<String> menuList = new ArrayList<>();
        ArrayAdapter<String> menuAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, menuList);
        mainMenu.setAdapter(menuAdapter);
        menuList.add(getString(R.string.last_played));
        menuList.add(getString(R.string.playlists));

        songsList = findViewById(R.id.songsList);

        // runtime check for permission
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST);

            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST);

            }
        } else {
            baseSongList = SongUtils.fillSongList(this, 0);
            PlaybackLogic.setSongsList(baseSongList);
            songAdapter = new SongAdapter(this, this, songsListView);
            songsList.setAdapter(songAdapter);
            songsListView.addAll(baseSongList);
            songAdapter.notifyDataSetChanged();
            startThreads();
        }

        createNotificationChannel();

        mainMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        Intent lastPlayedIntent = new Intent(MainActivity.this, LastPlayedActivity.class);
                        startActivity(lastPlayedIntent);
                        break;
                    case 1:
                        Intent playlistIntent = new Intent(MainActivity.this, PlaylistActivity.class);
                        startActivity(playlistIntent);
                        break;
                }
            }
        });

        songsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                int iterator = 0;
                long songID = songsListView.get(position).getSongID();
                for (int i = 0; i < baseSongList.size(); i++) {
                    if (baseSongList.get(i).getSongID() == songID) {
                        iterator = i;
                    }
                }

                if (PlaybackLogic.isPlayingPlaylist() || listChanged) {
                    PlaybackLogic.setSongsList(baseSongList);
                    PlaybackLogic.setPlayingPlaylist(false);
                    listChanged = false;
                }
                PlaybackLogic.setSongIterator(iterator);
                Bundle bundle = new Bundle();
                bundle.putParcelable("SONG_TO_PLAY", baseSongList.get(iterator));
                MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(String.valueOf(baseSongList.get(iterator).getSongID())
                        , bundle);
                PlaybackLogic.getPreviouslyPlayed().add(baseSongList.get(iterator));
            }
        });
        // set default values for sleep settings
        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);
    }


    @Override
    protected void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    {
                        if (ContextCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(this, "permission granted", Toast.LENGTH_LONG).show();
                            baseSongList = SongUtils.fillSongList(this, 0);
                            PlaybackLogic.setSongsList(baseSongList);
                            songAdapter = new SongAdapter(this, this, songsListView);
                            songsList.setAdapter(songAdapter);
                            songsListView.addAll(baseSongList);
                            songAdapter.notifyDataSetChanged();
                            startThreads();
                        } else {
                            Toast.makeText(this, "no permission granted", Toast.LENGTH_LONG).show();
                            finish();
                        }

                    }
                    return;
                }
        }
    }

    @Override
    public void onBackPressed() {
        //pressing back button makes app go background
        //moveTaskToBack(true);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate();
        } else super.onBackPressed();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("MusicID", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after thisenter code here
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbuttons, menu);
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (songsListView != null) {
            songsListView.clear();
        }
        for (int i = 0; i < baseSongList.size(); i++) {
            if (baseSongList.get(i).getSongName().toUpperCase().contains(newText.toUpperCase()) ||
                    baseSongList.get(i).getArtistName().toUpperCase().contains(newText.toUpperCase())) {
                songsListView.add(new Song(baseSongList.get(i).getSongName(),
                        baseSongList.get(i).getArtistName(),
                        baseSongList.get(i).getAlbum(),
                        baseSongList.get(i).getSongID(),
                        baseSongList.get(i).getPath()));
            }
        }
        songAdapter.notifyDataSetChanged();
        return false;
    }

    //Quick menu toolbar onClick method
    public void onClick(View view) {
        if (view.getId() == R.id.action_shuffle || view.getId() == R.id.shuffle_text) {
            playShuffle = !playShuffle;
            PlaybackLogic.setShuffle(playShuffle);
            PlaybackLogic.removeHistory();
            Toast.makeText(MainActivity.this, "Shuffle is " + playShuffle, Toast.LENGTH_SHORT).show();
        } else if (view.getId() == R.id.action_sort) {
            //creating dialog to choose sort type
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.sort_songs)
                    .setItems(R.array.sort_type, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            stopThreads.set(true);
                            songsListView.clear();
                            switch (whichSortType) {
                                case 0:
                                    if (which == 0) {
                                        Collections.reverse(baseSongList);
                                        songsListView.addAll(baseSongList);
                                    } else if (which == 1) {
                                        whichSortType = 1;
                                        baseSongList.clear();
                                        baseSongList = SongUtils.fillSongList(MainActivity.this, whichSortType);
                                        songsListView.addAll(baseSongList);
                                        stopThreads.set(false);
                                        startThreads();
                                    }
                                    break;
                                case 1:
                                    if (which == 0) {
                                        whichSortType = 0;
                                        baseSongList.clear();
                                        baseSongList = SongUtils.fillSongList(MainActivity.this, whichSortType);
                                        songsListView.addAll(baseSongList);
                                        stopThreads.set(false);
                                        startThreads();
                                    } else if (which == 1) {
                                        Collections.reverse(baseSongList);
                                        songsListView.addAll(baseSongList);
                                    }
                                    break;
                            }
                            listChanged = true;
                            songAdapter.notifyDataSetChanged();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void startThreads() {
        int chunkSize = 100;
        for (int i = 0; i < songsListView.size(); i += chunkSize) {
            LoadCovers loadCovers = new LoadCovers(songsListView, mHandler, i, Math.min(i + chunkSize, songsListView.size()), 90, 90, true);
            loadCovers.start();
            threadCount++;
        }
    }
}



