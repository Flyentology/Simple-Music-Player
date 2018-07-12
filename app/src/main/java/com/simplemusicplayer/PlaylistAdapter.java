package com.simplemusicplayer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
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
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.playlist_item, parent, false);
        }

        ImageView image = convertView.findViewById(R.id.imageView_coverPlaylist);
        if(playlist.getPlaylistSongs().size() > 0){
            for(int i = 0; i<playlist.getPlaylistSongs().size(); i++)
            {
                if (playlist.getPlaylistSongs().get(i).getAlbumArt() != null) {
                    image.setImageBitmap(playlist.getPlaylistSongs().get(position).getAlbumArt());
                }
            }
        } else {
            image.setBackgroundColor(Color.parseColor("#D3D3D3"));
            image.setImageResource(R.drawable.ic_empty_cover);
        }

        TextView name = convertView.findViewById(R.id.textView_playlistName);
        name.setText(playlist.getName());

        TextView songsCount = convertView.findViewById(R.id.textView_songsCount);
        songsCount.setText(Resources.getSystem().getString(R.string.playlist_count) + String.valueOf(playlist.getCount()));

        return convertView;
    }
}
