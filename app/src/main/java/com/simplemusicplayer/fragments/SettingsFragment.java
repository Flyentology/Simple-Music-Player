package com.simplemusicplayer.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SeekBarPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplemusicplayer.R;
import com.simplemusicplayer.SleepTask;

import java.util.concurrent.TimeUnit;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_settings);
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
