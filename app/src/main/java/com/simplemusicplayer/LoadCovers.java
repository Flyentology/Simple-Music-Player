package com.simplemusicplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Handler;

import java.util.ArrayList;


class LoadCovers extends Thread {

    private int initialValue;
    private int finalValue;
    private int desiredWidth;
    private int desiredHeight;
    private ArrayList<Song> baseSongList;
    private Handler handler;
    private boolean loadMultipleCovers;
    private Playlist playlist;

    LoadCovers(ArrayList<Song> baseSongList, Handler handler, int initialValue, int finalValue, int desiredWidth, int desiredHeight, boolean loadMultipleCovers) {
        this.baseSongList = baseSongList;
        this.handler = handler;
        this.initialValue = initialValue;
        this.finalValue = finalValue;
        this.desiredWidth = desiredWidth;
        this.desiredHeight = desiredHeight;
        this.loadMultipleCovers = loadMultipleCovers;
    }

    // Constructor for loading single image
    LoadCovers(Handler handler, Playlist playlist, int desiredWidth, int desiredHeight) {
        this.handler = handler;
        this.baseSongList = playlist.getPlaylistSongs();
        this.playlist = playlist;
        this.initialValue = 0;
        this.finalValue = playlist.getPlaylistSongs().size();
        this.desiredWidth = desiredWidth;
        this.desiredHeight = desiredHeight;
        this.loadMultipleCovers = false;
    }

    public void run() {
        for (int i = initialValue; i < finalValue; i++) {
            try {
                MediaMetadataRetriever metaRetreiver = new MediaMetadataRetriever();
                metaRetreiver.setDataSource(baseSongList.get(i).getPath());
                byte[] art = metaRetreiver.getEmbeddedPicture();
                if (art != null) {
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inJustDecodeBounds = true; //just check size of image
                    BitmapFactory.decodeByteArray(art, 0, art.length, opt);

                    // assign values of image
                    int imageHeight = opt.outHeight;
                    int imageWidth = opt.outWidth;

                    //condition to determine max inSample size
                    if (imageHeight > desiredHeight || imageWidth > desiredWidth) {
                        final int halfHeight = imageHeight / 2;
                        final int halfWidth = imageWidth / 2;
                        int inSampleSize = 1;
                        while ((halfHeight / inSampleSize) >= desiredHeight
                                && (halfWidth / inSampleSize) >= desiredWidth) {
                            inSampleSize *= 2;
                        }
                        opt.inSampleSize = inSampleSize;
                    }
                    opt.inJustDecodeBounds = false;
                    Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length, opt);

                    if (loadMultipleCovers) {
                        baseSongList.get(i).setCoverArt(songImage);
                    } else if (playlist != null) {
                        playlist.setPlaylistArt(songImage);
                        break;
                    } else {
                        baseSongList.get(i).setCoverArt(songImage);
                        handler.obtainMessage(i).sendToTarget();
                        return;
                    }
                }
                if (MainActivity.stopThreads.get()) {
                    break;
                }
            } catch (Exception e) {
            }
        }
        handler.obtainMessage(0).sendToTarget();
    }
}