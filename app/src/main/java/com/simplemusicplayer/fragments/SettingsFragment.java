package com.simplemusicplayer.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SeekBarPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplemusicplayer.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_settings);

        SeekBarPreference seekBarPreference = (SeekBarPreference) findPreference("Title");
        if (seekBarPreference != null) {
            seekBarPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue instanceof Integer) {
                        Integer newValueInt;
                        try {
                            newValueInt = (Integer) newValue;
                        } catch (NumberFormatException nfe) {
                            return false;
                        }
                        // Do something with the value
                        return true;
                    } else {
                        String objType = newValue.getClass().getName();
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
