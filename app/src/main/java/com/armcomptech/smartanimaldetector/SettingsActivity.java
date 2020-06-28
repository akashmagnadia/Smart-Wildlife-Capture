package com.armcomptech.smartanimaldetector;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

public class SettingsActivity extends AppCompatActivity {

    public static int getDefaultConfidenceLevel() {
        return defaultConfidenceLevel;
    }

    static int defaultConfidenceLevel = 60;


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
            CheckBoxPreference mDefaultGeneralBoxCheckBox = findPreference("defaultGeneralBoxCheckBox");
            SeekBarPreference mGeneralBoxSeekBar = findPreference("generalBoxSeekBar");

            mDefaultGeneralBoxCheckBox.setOnPreferenceChangeListener((preference, newValue) -> {
                if(mDefaultGeneralBoxCheckBox.isChecked()) {
                    mGeneralBoxSeekBar.setValue(getDefaultConfidenceLevel());
                } else {
                    mGeneralBoxSwitch.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + getDefaultConfidenceLevel() + "% confidence");
                }
                return true;
            });

            mGeneralBoxSwitch.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + mGeneralBoxSeekBar.getValue() + "% confidence");
            mGeneralBoxSeekBar.setOnPreferenceChangeListener((preference, newValue) -> {
                mGeneralBoxSwitch.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + newValue + "% confidence");
                return true;
            });

            SwitchPreference mGeneralSwitchTakePhoto = findPreference("generalSwitchTakePhoto");

            SwitchPreference mBirdSwitchTakePhoto = findPreference("birdSwitchTakePhoto");
            CheckBoxPreference mDefaultBirdTakePhotoCheckBox = findPreference("defaultBirdTakePhotoCheckBox");
            SeekBarPreference mBirdSeekBar = findPreference("birdSeekBar");

            mDefaultBirdTakePhotoCheckBox.setOnPreferenceChangeListener((preference, newValue) -> {
                if(mDefaultBirdTakePhotoCheckBox.isChecked()) {
                    mBirdSeekBar.setValue(getDefaultConfidenceLevel());
                } else {
                    mBirdSwitchTakePhoto.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + getDefaultConfidenceLevel() + "% confidence");
                }
                return true;
            });

            mBirdSwitchTakePhoto.setSummaryOn("Photos of the birds will be taken if the machine detects with " + mBirdSeekBar.getValue() + "% confidence");
            mBirdSeekBar.setOnPreferenceChangeListener((preference, newValue) -> {
                mBirdSwitchTakePhoto.setSummaryOn("Photos of the birds will be taken if the machine detects with " + newValue + "% confidence");
                return true;
            });

            SwitchPreference mSquirrelSwitchTakePhoto = findPreference("squirrelSwitchTakePhoto");
            CheckBoxPreference mDefaultSquirrelTakePhotoCheckBox = findPreference("defaultSquirrelTakePhotoCheckBox");
            SeekBarPreference mSquirrelSeekBar = findPreference("squirrelSeekBar");

            mDefaultSquirrelTakePhotoCheckBox.setOnPreferenceChangeListener((preference, newValue) -> {
                if(mDefaultSquirrelTakePhotoCheckBox.isChecked()) {
                    mSquirrelSeekBar.setValue(getDefaultConfidenceLevel());
                } else {
                    mSquirrelSwitchTakePhoto.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + getDefaultConfidenceLevel() + "% confidence");
                }
                return true;
            });

            mSquirrelSwitchTakePhoto.setSummaryOn("Photos of the squirrels will be taken if the machine detects with " + mSquirrelSeekBar.getValue() + "% confidence");
            mSquirrelSeekBar.setOnPreferenceChangeListener((preference, newValue) -> {
                mSquirrelSwitchTakePhoto.setSummaryOn("Photos of the squirrels will be taken if the machine detects with " + newValue + "% confidence");
                return true;
            });
        }
    }
}