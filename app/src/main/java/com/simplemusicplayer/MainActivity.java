package com.simplemusicplayer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {


    private MediaPlayerHolder mediaPlayerHolder;
    private final int MY_PERMISSIONS_REQUEST = 1;
    private ListView songsList;
    private boolean playShuffle = false;
    private MediaSession mediaSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        songsList = findViewById(R.id.songsList);
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
            fillListView();
            ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fillListView());
            songsList.setAdapter(arrayAdapter);
        }

        createNotificationChannel();
        songsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                mediaPlayerHolder.reset();
                mediaPlayerHolder.setSongIterator(position);
                mediaPlayerHolder.loadMedia();
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
                            fillListView();
                            ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fillListView());
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


    private ArrayList fillListView() {
        ArrayList<String> songList = new ArrayList<>();
        for (int i = 0; i < mediaPlayerHolder.getSongsList().size() - 1; i++) {
            String songName = mediaPlayerHolder.getSongsList().get(i).getSongName();
            String artistName = mediaPlayerHolder.getSongsList().get(i).getArtistName();
            //String albumName = mediaPlayerHolder.getSongsList().get(i).getAlbum();
            songList.add(artistName + " - " + songName);
        }
        return songList;
    }

    public void createNotification(String songName, String currentState) {

        // preparing to add notifications
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //TODO: Notification fix, and register clicks using broadcaster
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
                .setOngoing(true) //user can't remove notification
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
                playShuffle = !playShuffle;
                mediaPlayerHolder.setShuffle(playShuffle);
                mediaPlayerHolder.removeHistory(playShuffle);
                Toast.makeText(this, "Shuffle is " + playShuffle, Toast.LENGTH_LONG).show();
                return true;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbuttons, menu);
        return true;
    }
}



