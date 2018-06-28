package com.simplemusicplayer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class PlaylistAdapter extends BaseAdapter {

    private ArrayList<Playlist> playlists;
    private Context context;

    public PlaylistAdapter(Context context, ArrayList<Playlist> playlists) {
        this.playlists = playlists;
        this.context = context;
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
        TextView playlistName;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            playlistName = new TextView(context);
            playlistName.setMinWidth(100);
            playlistName.setLayoutParams(new ViewGroup.LayoutParams(85, 85));
            playlistName.setPadding(8, 8, 8, 8);
        } else {
            playlistName = (TextView) convertView;
        }

        playlistName.setText(playlists.get(position).toString());
        return playlistName;
    }
}
