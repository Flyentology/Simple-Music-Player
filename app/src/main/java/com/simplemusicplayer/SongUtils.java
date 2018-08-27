package com.simplemusicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import com.simplemusicplayer.models.Song;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used for utility methods.
 */
public class SongUtils {

    /**
     * Method that queries for media files in a phone and returns list of songs depends of sort type.
     */
    public static List<Song> fillSongList(Context context, int sortOrder) {
        List<Song> songsList = new ArrayList<>();
        Cursor cursor;
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; //uri is basically URL, so points to a place in the phone where media is stored
        //choose sort order
        if (sortOrder == 0) {
            cursor = contentResolver.query(uri, null, null, null, MediaStore.Audio.Media.DATE_ADDED + " COLLATE NOCASE ASC"); //query for audio files on the phone
        } else {
            cursor = contentResolver.query(uri, null, null, null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"); //query for audio files on the phone
        }
        if (cursor == null) {
            // query failed
        } else if (!cursor.moveToFirst()) {
            //no media on the device
        } else {
            // get index of each parameter of audio file
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int albumIDColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            do {
                long id = cursor.getLong(idColumn);
                long albumID = cursor.getLong(albumIDColumn);
                String thisTitle = cursor.getString(titleColumn);
                String artistName = cursor.getString(artistColumn);
                String albumName = cursor.getString(albumColumn);
                String path = "content://media/external/audio/albumart/" + albumID;
                String pathToFile = cursor.getString(pathColumn);

                //add songs to the list
                songsList.add(new Song(thisTitle, artistName, albumName, id, path, pathToFile));
            } while (cursor.moveToNext());
            cursor.close();
            //cursors should be freed up after use
        }
        return songsList;
    }
}
