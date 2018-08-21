package com.simplemusicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.simplemusicplayer.models.Song;

import java.util.ArrayList;
import java.util.List;

/**Class used for utility methods.*/
public class SongUtils {

    /**Method that queries for media files in a phone and returns list of songs depends of sort type.*/
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
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            do {
                long id = cursor.getLong(idColumn);
                String thisTitle = cursor.getString(titleColumn);
                String artistName = cursor.getString(artistColumn);
                String albumName = cursor.getString(albumColumn);
                String path = cursor.getString(dataColumn);
                //add songs to the list
                songsList.add(new Song(thisTitle, artistName, albumName, id, path));
            } while (cursor.moveToNext());
            cursor.close();
            //cursors should be freed up after use
        }
        return songsList;
    }

    /**
     * Nested class used to asynchronously load one cover art bitmap.
     * Size is passed in constructor.
     */
    public static class LoadCover extends AsyncTask<String, Integer, Bitmap> {

        private int width;
        private int height;

        public interface AsyncResponse {
            public void processFinish(Bitmap output);
        }

        public AsyncResponse delegate = null;

        public LoadCover(int width, int height, AsyncResponse delegate) {
            this.width = width;
            this.height = height;
            this.delegate = delegate;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            String path = strings[0];
            MediaMetadataRetriever metaRetreiver = new MediaMetadataRetriever();
            metaRetreiver.setDataSource(path);
            byte[] art = metaRetreiver.getEmbeddedPicture();
            if (art != null) {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inJustDecodeBounds = true; //just check size of image
                BitmapFactory.decodeByteArray(art, 0, art.length, opt);

                // assign values of image
                int imageHeight = opt.outHeight;
                int imageWidth = opt.outWidth;

                //condition to determine max inSample size
                if (imageHeight > height || imageWidth > width) {
                    final int halfHeight = imageHeight / 2;
                    final int halfWidth = imageWidth / 2;
                    int inSampleSize = 1;
                    while ((halfHeight / inSampleSize) >= height
                            && (halfWidth / inSampleSize) >= width) {
                        inSampleSize *= 2;
                    }
                    opt.inSampleSize = inSampleSize;
                }
                opt.inJustDecodeBounds = false;
                return BitmapFactory.decodeByteArray(art, 0, art.length, opt);
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            delegate.processFinish(bitmap);
        }
    }
}
