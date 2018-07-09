package com.simplemusicplayer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, MediaController.MediaPlayerControl {

    private final int MY_PERMISSIONS_REQUEST = 1;
    private ListView songsList;
    private boolean playShuffle = false;
    private ArrayAdapter arrayAdapter;
    private ArrayList<Song> songsListView = new ArrayList<>();
    private ArrayList<Song> baseSongList = new ArrayList<>();
    private boolean mBound = false;
    MediaPlayerHolder mediaPlayerHolder;
    private MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        mediaController = new MediaController(this);

        ListView mainMenu = findViewById(R.id.mainMenu);
        ArrayList<String> menuList = new ArrayList<>();
        ArrayAdapter<String> menuAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, menuList);
        mainMenu.setAdapter(menuAdapter);
        menuList.add(getString(R.string.last_played));
        menuList.add(getString(R.string.favorites));
        menuList.add(getString(R.string.playlists));

        songsList = findViewById(R.id.songsList);
        songsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        /*
        Registering receiver to receive songsList
         */
        receiveFromService receive = new receiveFromService();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("SONG_LIST");
        intentFilter.addAction("MEDIA_CONTROLLER");
        intentFilter.addAction("MEDIA_CONTROLLER");
        getApplicationContext().registerReceiver(receive, intentFilter);

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
            //startService(new Intent(this, MediaPlayerHolder.class));
            Intent intent = new Intent(this, MediaPlayerHolder.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, songsListView);
            songsList.setAdapter(arrayAdapter);
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
                        Intent favoritesIntent = new Intent(MainActivity.this, FavoritesActivity.class);
                        startActivity(favoritesIntent);
                        break;
                    case 2:
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
                Intent intent = new Intent("PLAY_SONG");
                intent.putExtra("ITERATOR",iterator);
                intent.putExtra("ID", songID);
                sendBroadcast(intent);
            }
        });

    }

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerHolder.LocalBinder binder = (MediaPlayerHolder.LocalBinder) service;
            mediaPlayerHolder = binder.getService();
            mBound = true;

            /*
            Bind service call is asynchronous so we start media controller after it's successfully
            bound to our service class
             */
            if(mediaPlayerHolder!=null){
                setMediaController();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStop(){
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //if (//mediaPlayerHolder.getMediaPlayer() != null) {
        //mediaPlayerHolder.getMediaPlayer().release();
        //}
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
                            //startService(new Intent(this, MediaPlayerHolder.class));
                            Intent intent = new Intent(this, MediaPlayerHolder.class);
                            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                            arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, songsListView);
                            songsList.setAdapter(arrayAdapter);
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
        moveTaskToBack(true);
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
            // or other notification behaviors after this
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
                        baseSongList.get(i).getSongID()));
            }
        }
        arrayAdapter.notifyDataSetChanged();
        return false;
    }

    @Override
    public void start() {
        mediaPlayerHolder.getMediaPlayer().start();
    }

    @Override
    public void pause() {
        mediaPlayerHolder.getMediaPlayer().pause();
    }

    @Override
    public int getDuration() {
        return mediaPlayerHolder.getMediaPlayer().getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayerHolder.getMediaPlayer().getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mediaPlayerHolder.getMediaPlayer().seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayerHolder.getMediaPlayer().isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public void setMediaController(){
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(findViewById(R.id.mainView));
        mediaController.setEnabled(true);
        mediaController.show(0);
        mediaController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mediaPlayerHolder.nextSong();
                mediaPlayerHolder.loadMedia(mediaPlayerHolder.getSongsList().get(mediaPlayerHolder.getSongIterator()).getSongID());
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayerHolder.previousSong();
                mediaPlayerHolder.loadMedia(mediaPlayerHolder.getSongsList().get(mediaPlayerHolder.getSongIterator()).getSongID());
            }
        });
    }

    private class receiveFromService extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("SONG_LIST")){
                baseSongList = (ArrayList<Song>) intent.getSerializableExtra("songsList");
                songsListView.addAll(baseSongList);
                arrayAdapter.notifyDataSetChanged();
            } else if(intent.getAction().equals("MEDIA_CONTROLLER")){
            }

        }
    }
    //Quick menu toolbar onClick method
    public void onClick(View view) {
        if (view.getId() == R.id.action_shuffle || view.getId() == R.id.shuffle_text) {
                playShuffle = !playShuffle;
                Intent shuffleIntent = new Intent("SHUFFLE");
                shuffleIntent.putExtra("ShuffleBoolean", playShuffle);
                sendBroadcast(shuffleIntent);
                Toast.makeText(MainActivity.this, "Shuffle is " + playShuffle, Toast.LENGTH_SHORT).show();
        } else if (view.getId() == R.id.action_sort) {
            //creating dialog to choose sort type
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.sort_songs)
                    .setItems(R.array.sort_type, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            songsListView.clear();
                            baseSongList.clear();
                            Intent sortIntent = new Intent("SORT_TYPE");
                            sortIntent.putExtra("SORT", which);
                            sendBroadcast(sortIntent);
                            //clear previous list

                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}



