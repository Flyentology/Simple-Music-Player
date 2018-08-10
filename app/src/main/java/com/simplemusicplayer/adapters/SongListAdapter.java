package com.simplemusicplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Song;

import java.util.ArrayList;

public class SongListAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Song> songs;

    public SongListAdapter(Context mContext, ArrayList<Song> songs) {
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
        protected CheckBox checkbox;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            // if it's not recycled, initialize some attributes
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            view = layoutInflater.inflate(R.layout.add_songs_list_item, viewGroup, false);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.itemCount = view.findViewById(R.id.playlist_view_itemCount);
            viewHolder.songTitle = view.findViewById(R.id.playlist_view_songTitle);
            viewHolder.artistName = view.findViewById(R.id.playlist_view_artistName);
            viewHolder.checkbox = view.findViewById(R.id.checkboxSong);
            viewHolder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    Song song = (Song) viewHolder.checkbox.getTag();
                    song.setSelected(compoundButton.isChecked());
                }
            });
            view.setTag(viewHolder);
            viewHolder.checkbox.setTag(songs.get(i));
        } else {
            ((ViewHolder) view.getTag()).checkbox.setTag(songs.get(i));
        }

        ViewHolder holder = (ViewHolder) view.getTag();

        if (i < 10) {
            holder.itemCount.setText(String.format("%02d", (i + 1)));

        } else {
            holder.itemCount.setText(String.valueOf(i + 1));
        }

        holder.songTitle.setText(songs.get(i).getSongName());
        holder.artistName.setText(songs.get(i).getArtistName());
        holder.checkbox.setChecked(songs.get(i).isSelected());
        return view;
    }

}
