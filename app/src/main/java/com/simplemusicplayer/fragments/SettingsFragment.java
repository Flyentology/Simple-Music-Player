package com.simplemusicplayer.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SeekBarPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.simplemusicplayer.R;
import com.simplemusicplayer.SleepTask;

import java.util.concurrent.TimeUnit;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_settings);

        android.support.v14.preference.SwitchPreference switchPreference = (android.support.v14.preference.SwitchPreference) findPreference("pref_sleep_audio");
        if (switchPreference != null) {
            switchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue instanceof Boolean) {
                        boolean newValueBoolean;
                        try {
                            newValueBoolean = (boolean) newValue;
                        } catch (NumberFormatException nfe) {
                            return false;
                        }
                        if (!newValueBoolean) {
                            WorkManager.getInstance().cancelAllWork();
                            Toast.makeText(getActivity(), "Playback will continue", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        SeekBarPreference seekBarPreference = (SeekBarPreference) findPreference("pref_seekbar_position");
        if (seekBarPreference != null) {
            seekBarPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    WorkManager.getInstance().cancelAllWork();
                    if (newValue instanceof Integer) {
                        Integer newValueInt;
                        try {
                            newValueInt = (Integer) newValue;
                        } catch (NumberFormatException nfe) {
                            return false;
                        }
                        OneTimeWorkRequest sleepTask = new OneTimeWorkRequest.Builder(SleepTask.class).setInitialDelay(newValueInt, TimeUnit.MINUTES).build();
                        WorkManager.getInstance().enqueue(sleepTask);
                        Toast.makeText(getActivity(), "Playback will be stopped in " + newValueInt + " minutes", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            view.setBackgroundColor(getResources().getColor(R.color.colorToolbar));
        }
        return view;
    }
}
