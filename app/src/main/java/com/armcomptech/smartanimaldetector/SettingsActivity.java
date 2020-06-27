package com.armcomptech.smartanimaldetector;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            SwitchPreference mGeneralBoxSwitch = findPreference("generalBoxSwitch");
            SeekBarPreference mGeneralBoxSeekBar = findPreference("generalBoxSeekBar");

            SwitchPreference mGeneralSwitchTakePhoto = findPreference("generalSwitchTakePhoto");

            SwitchPreference mBirdSwitchTakePhoto = findPreference("birdSwitchTakePhoto");
            SeekBarPreference mBirdSeekBar = findPreference("birdSeekBar");

            SwitchPreference mSquirrelSwitchTakePhoto = findPreference("squirrelSwitchTakePhoto");
            SeekBarPreference mSquirrelSeekBar = findPreference("squirrelSeekBar");

            if (mGeneralBoxSwitch != null) mGeneralBoxSwitch.setVisible(true);
            if (mGeneralBoxSeekBar != null) mGeneralBoxSeekBar.setVisible(true);
            if (mGeneralSwitchTakePhoto != null) mGeneralSwitchTakePhoto.setVisible(true);
            if (mBirdSwitchTakePhoto != null) mBirdSwitchTakePhoto.setVisible(true);
            if (mBirdSeekBar != null) mBirdSeekBar.setVisible(true);
            if (mSquirrelSwitchTakePhoto != null) mSquirrelSwitchTakePhoto.setVisible(true);
            if (mSquirrelSeekBar != null) mSquirrelSeekBar.setVisible(true);
        }
    }
}