package com.armcomptech.smartanimaldetector;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
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

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SwitchPreference mGeneralBoxSwitch = findPreference("generalBoxSwitch");
            SeekBarPreference mGeneralBoxSeekBar = findPreference("generalBoxSeekBar");

            mGeneralBoxSwitch.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + mGeneralBoxSeekBar.getValue() + "% confidence");
            mGeneralBoxSeekBar.setOnPreferenceChangeListener((preference, newValue) -> {
                mGeneralBoxSwitch.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + newValue + "% confidence");
                return true;
            });

            SwitchPreference mGeneralSwitchTakePhoto = findPreference("generalSwitchTakePhoto");

            SwitchPreference mBirdSwitchTakePhoto = findPreference("birdSwitchTakePhoto");
            SeekBarPreference mBirdSeekBar = findPreference("birdSeekBar");

            mBirdSwitchTakePhoto.setSummaryOn("Photos of the birds will be taken if the machine detects with " + mBirdSeekBar.getValue() + "% confidence");
            mBirdSeekBar.setOnPreferenceChangeListener((preference, newValue) -> {
                mBirdSwitchTakePhoto.setSummaryOn("Photos of the birds will be taken if the machine detects with " + newValue + "% confidence");
                return true;
            });

            SwitchPreference mSquirrelSwitchTakePhoto = findPreference("squirrelSwitchTakePhoto");
            SeekBarPreference mSquirrelSeekBar = findPreference("squirrelSeekBar");

            mSquirrelSwitchTakePhoto.setSummaryOn("Photos of the squirrels will be taken if the machine detects with " + mSquirrelSeekBar.getValue() + "% confidence");
            mSquirrelSeekBar.setOnPreferenceChangeListener((preference, newValue) -> {
                mSquirrelSwitchTakePhoto.setSummaryOn("Photos of the squirrels will be taken if the machine detects with " + newValue + "% confidence");
                return true;
            });
        }
    }
}