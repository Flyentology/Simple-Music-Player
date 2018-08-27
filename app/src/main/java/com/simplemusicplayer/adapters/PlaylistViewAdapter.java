package com.simplemusicplayer.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.simplemusicplayer.R;
import com.simplemusicplayer.activities.PlaylistActivity;
import com.simplemusicplayer.models.Playlist;
import com.simplemusicplayer.models.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PlaylistViewAdapter extends BaseAdapter {
    private Context mContext;
    private List<Song> songs;
    private List<Playlist> playlists;

    public PlaylistViewAdapter(Context mContext, List<Song> songs, List<Playlist> playlists) {
        this.mContext = mContext;
        this.songs = songs;
        this.playlists = playlists;
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int i) {
        return songs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    static class ViewHolder {
        protected TextView itemCount, songTitle, artistName;
        protected ImageButton moreOptions;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            // if it's not recycled, initialize some attributes
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            view = layoutInflater.inflate(R.layout.playlist_view_item, viewGroup, false);
            final PlaylistViewAdapter.ViewHolder viewHolder = new PlaylistViewAdapter.ViewHolder();
            viewHolder.itemCount = view.findViewById(R.id.playlist_view_itemCount);
            viewHolder.songTitle = view.findViewById(R.id.playlist_view_songTitle);
            viewHolder.artistName = view.findViewById(R.id.playlist_view_artistName);
            viewHolder.moreOptions = view.findViewById(R.id.moreOptions);
            view.setTag(viewHolder);
            viewHolder.moreOptions.setTag(i);
        } else {
            ((ViewHolder) view.getTag()).moreOptions.setTag(songs.get(i));
        }

        PlaylistViewAdapter.ViewHolder holder = (PlaylistViewAdapter.ViewHolder) view.getTag();

        final int viewPosition = i;
        holder.moreOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(songs.get(viewPosition).getSongName())
                        .setItems(R.array.edit_song_dialog, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                switch (which) {
                                    case 0:
                                        List<String> playlistNames = new ArrayList<>();

                                        if(playlists.size() > 0){
                                            for (Playlist playlist : playlists) {
                                                playlistNames.add(playlist.toString());
                                            }
                                        }
                                        final CharSequence[] cs = playlistNames.toArray(new CharSequence[playlists.size()]);
                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(mContext);
                                        builder1.setTitle(R.string.add_to)
                                                .setItems(cs, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        // check if playlist contains song and save it
                                                        if (!playlists.get(i).getPlaylistSongs().contains(songs.get(viewPosition))) {
                                                            playlists.get(i).getPlaylistSongs().add(songs.get(viewPosition));
                                                            PlaylistActivity.writeJSON(playlists, mContext);
                                                        }
                                                    }
                                                });
                                        AlertDialog dialog1 = builder1.create();
                                        dialog1.show();
                                        break;
                                    case 1:
                                        songs.remove(viewPosition);
                                        notifyDataSetChanged();
                                        break;
                                    case 2:
                                        String filePath = songs.get(viewPosition).getPathToFile();
                                        Uri uri = FileProvider.getUriForFile(mContext, "com.simplemusicplayer.fileprovider", new File(filePath));
                                        Intent share = new Intent(Intent.ACTION_SEND);
                                        share.setType("audio/*");
                                        share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        share.putExtra(Intent.EXTRA_STREAM, uri);
                                        mContext.startActivity(Intent.createChooser(share, "Share Sound File"));
                                        break;
                                }
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        if (i < 10) {
            holder.itemCount.setText(String.format(Locale.US,"%02d", (i + 1)));

        } else {
            holder.itemCount.setText(String.valueOf(i + 1));
        }
        holder.songTitle.setText(songs.get(i).getSongName());
        holder.artistName.setText(songs.get(i).getArtistName());
        return view;
    }
}
