package com.simplemusicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class PlaylistAdapter extends BaseAdapter {

    private ArrayList<Playlist> playlists;
    private Context mContext;

    PlaylistAdapter(Context context, ArrayList<Playlist> playlists) {
        this.playlists = playlists;
        this.mContext = context;
    }

    static class ViewHolder {
        protected TextView name, songsCount;
        protected ImageView image;
    }

    @Override
    public int getCount() {
        return playlists.size();
    }

    @Override
    public Object getItem(int position) {
        return playlists.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Playlist playlist = playlists.get(position);
        ViewHolder holder;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.playlist_item, parent, false);

            holder = new ViewHolder();

            holder.image = convertView.findViewById(R.id.imageView_coverPlaylist);
            holder.name = convertView.findViewById(R.id.textView_playlistName);
            holder.songsCount = convertView.findViewById(R.id.textView_songsCount);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();

            if (playlist.getPlaylistArt() != null) {
                holder.image.setImageBitmap(playlist.getPlaylistArt());
            } else {
                holder.image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_empty_cover));
            }

            holder.name.setText(playlist.getName());
            holder.songsCount.setText(mContext.getResources().getString(R.string.playlist_count) + Integer.toString(playlist.getCount()));
        }
        return convertView;
    }

}
