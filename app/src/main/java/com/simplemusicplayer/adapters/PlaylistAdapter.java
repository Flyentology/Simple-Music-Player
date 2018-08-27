package com.simplemusicplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.simplemusicplayer.models.Playlist;
import com.simplemusicplayer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that extends BaseAdapter and returns view of playlist item on grid view.
 */
public class PlaylistAdapter extends BaseAdapter {

    private List<Playlist> playlists;
    private Context mContext;

    public PlaylistAdapter(Context context, List<Playlist> playlists) {
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

            if(playlist.getPathToCover() != null){
                Glide.with(mContext).load(playlist.getPathToCover()).apply(new RequestOptions().override(200, 200)).into(holder.image);
            } else {
                Glide.with(mContext).load(R.drawable.ic_empty_cover).into(holder.image);
            }
            holder.name.setText(playlist.getName());
            holder.songsCount.setText(mContext.getResources().getString(R.string.playlist_count) + Integer.toString(playlist.getCount()));
        }
        return convertView;
    }

}
