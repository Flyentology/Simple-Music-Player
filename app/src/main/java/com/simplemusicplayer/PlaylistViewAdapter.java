package com.simplemusicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

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
            view.setTag(viewHolder);
        }
        PlaylistViewAdapter.ViewHolder holder = (PlaylistViewAdapter.ViewHolder) view.getTag();

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
