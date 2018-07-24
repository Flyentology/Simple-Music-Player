package com.simplemusicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

public class FillSongList {

    public static ArrayList fillSongList(Context context, int sortOrder) {
        ArrayList<Song> songsList = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; //uri is basically URL, so points to a place in the phone where media is stored
        Cursor cursor;
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
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            //add songs to the list
            do {
                long id = cursor.getLong(idColumn);
                String thisTitle = cursor.getString(titleColumn);
                String artistName = cursor.getString(artistColumn);
                String albumName = cursor.getString(albumColumn);
                String path = cursor.getString(dataColumn);
                songsList.add(new Song(thisTitle, artistName, albumName, id, path));
            } while (cursor.moveToNext());

            cursor.close();
            //cursors should be freed up after use
        }
        return songsList;
    }
}
