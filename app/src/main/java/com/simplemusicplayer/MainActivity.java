package com.simplemusicplayer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {


    private MediaPlayerHolder mediaPlayerHolder;
    private final int MY_PERMISSIONS_REQUEST = 1;
    private ListView songsList;
    private boolean playShuffle = false;
    private MediaSession mediaSession;
    private ArrayAdapter arrayAdapter;
    private ArrayList<Song> songsListView = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        //Toolbar quickMenu = findViewById(R.id.quickMenu);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Toolbar quickMenu = findViewById(R.id.quickMenu);

        ListView mainMenu = findViewById(R.id.mainMenu);
        ArrayList<String> menuList = new ArrayList<>();
        ArrayAdapter<String> menuAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, menuList);
        mainMenu.setAdapter(menuAdapter);
        menuList.add(getString(R.string.last_played));
        menuList.add(getString(R.string.favorites));
        menuList.add(getString(R.string.playlists));

        songsList = findViewById(R.id.songsList);
        songsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mediaPlayerHolder = new MediaPlayerHolder(this, MainActivity.this);
        mediaSession = new MediaSession(this, "Playback");

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
            mediaPlayerHolder.fillSongList();
            for (Song song : mediaPlayerHolder.getSongsList()) {
                songsListView.add(song);
            }
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

                long songID = songsListView.get(position).getSongID();
                for (int i = 0; i < mediaPlayerHolder.getSongsList().size(); i++) {
                    if (mediaPlayerHolder.getSongsList().get(i).getSongID() == songID) {
                        mediaPlayerHolder.setSongIterator(i);
                    }
                }
                mediaPlayerHolder.loadMedia(songID);
                mediaPlayerHolder.getPreviouslyPlayed().add(mediaPlayerHolder.getSongIterator());
                mediaPlayerHolder.getMediaController().show(0);
            }
        });
        mediaPlayerHolder.setMediaController();
        mediaPlayerHolder.getMediaController().setAnchorView((findViewById(R.id.mainView)));
        mediaPlayerHolder.getMediaController().setEnabled(true);
    }


    @Override
    protected void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayerHolder.getMediaPlayer() != null) {
            mediaPlayerHolder.getMediaPlayer().release();
        }
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
                            mediaPlayerHolder.fillSongList();
                            for (Song song : mediaPlayerHolder.getSongsList()) {
                                songsListView.add(song);
                            }
                            arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songsListView);
                            songsList.setAdapter(arrayAdapter);
                            Log.d("dddddd2", "" + mediaPlayerHolder.getSongsList().size());
                            //fillListView();
                            //ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, songList);
                            //songsList.setAdapter(arrayAdapter);
                        } else {
                            Toast.makeText(this, "no permission granted", Toast.LENGTH_LONG).show();
                            finish();
                        }

                    }
                    return;
                }
        }
    }

    public void createNotification(String songName, String currentState, boolean isPlaying) {

        // preparing to add notifications
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playNextIntent = new Intent();
        playNextIntent.setAction("PLAY_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 1, playNextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent();
        pauseIntent.setAction("PAUSE");
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playPreviousIntent = new Intent();
        playPreviousIntent.setAction("PLAY_PREVIOUS");
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 3, playPreviousIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "MusicID")
                .setSmallIcon(R.drawable.ic_launcher_background) //notification icon
                .setContentTitle("Simple Music Player")
                .setContentText("Currently playing: " + songName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(0)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) //visible on locked screen
                .setOngoing(isPlaying) //user can't remove notification
                .addAction(R.drawable.ic_previous, "Previous", prevPendingIntent) // #0
                .addAction(R.drawable.ic_pause, currentState, pausePendingIntent)  // #1
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent)// #2
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().setMediaSession(MediaSessionCompat.Token.fromToken(mediaSession.getSessionToken())))
                .setContentIntent(pendingIntent); //on click go to app intent
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, mBuilder.build()); //show notification
    }

    // now media controller can be showed since activity is launched
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mediaPlayerHolder.getMediaController() != null) {
            //mediaPlayerHolder.getMediaController().show(0);
        }
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

    /*
    Methods for creating menu for toolbar and to complete action when button is pressed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_shuffle:

        }
        return true;
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
        for (int i = 0; i < mediaPlayerHolder.getSongsList().size(); i++) {
            if (mediaPlayerHolder.getSongsList().get(i).getSongName().contains(newText) ||
                    mediaPlayerHolder.getSongsList().get(i).getArtistName().contains(newText)) {
                songsListView.add(new Song(mediaPlayerHolder.getSongsList().get(i).getSongName(),
                        mediaPlayerHolder.getSongsList().get(i).getArtistName(),
                        mediaPlayerHolder.getSongsList().get(i).getAlbum(),
                        mediaPlayerHolder.getSongsList().get(i).getSongID()));
            }
        }
        arrayAdapter.notifyDataSetChanged();
        return false;
    }

    public void onClick(View view) {
        if(view.getId() == R.id.action_shuffle || view.getId() == R.id.shuffle_text){
            playShuffle = !playShuffle;
            mediaPlayerHolder.setShuffle(playShuffle);
            mediaPlayerHolder.removeHistory();
            Toast.makeText(MainActivity.this, "Shuffle is " + playShuffle, Toast.LENGTH_LONG).show();
        } else {

        }
    }
}



