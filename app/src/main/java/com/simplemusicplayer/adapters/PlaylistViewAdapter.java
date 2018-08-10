package com.simplemusicplayer.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Song;

import java.util.ArrayList;

public class PlaylistViewAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<Song> songs;

    public PlaylistViewAdapter(Context mContext, ArrayList<Song> songs) {
        this.mContext = mContext;
        this.songs = songs;
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
                                switch (which){
                                    case 0:
                                        break;
                                    case 1:
                                        songs.remove(viewPosition);
                                        notifyDataSetChanged();
                                        break;
                                    case 2:
                                        break;
                                }
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        if (i < 10) {
            holder.itemCount.setText(String.format("%02d", (i + 1)));

        } else {
            holder.itemCount.setText(String.valueOf(i + 1));
        }
        holder.songTitle.setText(songs.get(i).getSongName());
        holder.artistName.setText(songs.get(i).getArtistName());
        return view;
    }
}
