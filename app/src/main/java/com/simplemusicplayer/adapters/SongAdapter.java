package com.simplemusicplayer.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.simplemusicplayer.R;
import com.simplemusicplayer.models.Song;

import java.util.List;

/**
 * Class extending ArrayAdapter used to display Song item in MainActivity.
 */
public class SongAdapter extends ArrayAdapter {

    private Context mContext;
    private List<Song> songList;

    public SongAdapter(@NonNull Context context, Context mContext, List<Song> songList) {
        super(context, 0, songList);
        this.mContext = mContext;
        this.songList = songList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.song_list_item, parent, false);

        Song currentSong = songList.get(position);
        ImageView image = listItem.findViewById(R.id.imageView_cover);
        Glide.with(mContext).load(currentSong.getPath()).apply(new RequestOptions().placeholder(R.drawable.ic_empty_cover).override(80,80)).into(image);

        TextView name = listItem.findViewById(R.id.textView_name);
        name.setText(currentSong.getSongName());

        TextView artist = listItem.findViewById(R.id.textView_artist);
        artist.setText(currentSong.getArtistName());

        return listItem;
    }
}
