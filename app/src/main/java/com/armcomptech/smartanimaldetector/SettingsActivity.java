package com.armcomptech.smartanimaldetector;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.CheckBoxPreference;
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

            assert mDefaultGeneralBoxCheckBox != null;
            mDefaultGeneralBoxCheckBox.setOnPreferenceChangeListener((preference, newValue) -> {
                if(mDefaultGeneralBoxCheckBox.isChecked()) {
                    assert mGeneralBoxSeekBar != null;
                    mGeneralBoxSeekBar.setValue(getDefaultConfidenceLevel());
                } else {
                    assert mGeneralBoxSwitch != null;
                    mGeneralBoxSwitch.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + getDefaultConfidenceLevel() + "% confidence");
                }
                return true;
            });

            assert mGeneralBoxSeekBar != null;
            assert mGeneralBoxSwitch != null;
            mGeneralBoxSwitch.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + mGeneralBoxSeekBar.getValue() + "% confidence");
            mGeneralBoxSeekBar.setOnPreferenceChangeListener((preference, newValue) -> {
                mGeneralBoxSwitch.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + newValue + "% confidence");
                return true;
            });

            SwitchPreference mBirdSwitchTakePhoto = findPreference("birdSwitchTakePhoto");
            CheckBoxPreference mDefaultBirdTakePhotoCheckBox = findPreference("defaultBirdTakePhotoCheckBox");
            SeekBarPreference mBirdSeekBar = findPreference("birdSeekBar");

            assert mDefaultBirdTakePhotoCheckBox != null;
            mDefaultBirdTakePhotoCheckBox.setOnPreferenceChangeListener((preference, newValue) -> {
                if(mDefaultBirdTakePhotoCheckBox.isChecked()) {
                    assert mBirdSeekBar != null;
                    mBirdSeekBar.setValue(getDefaultConfidenceLevel());
                } else {
                    assert mBirdSwitchTakePhoto != null;
                    mBirdSwitchTakePhoto.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + getDefaultConfidenceLevel() + "% confidence");
                }
                return true;
            });

            assert mBirdSeekBar != null;
            assert mBirdSwitchTakePhoto != null;
            mBirdSwitchTakePhoto.setSummaryOn("Photos of the birds will be taken if the machine detects with " + mBirdSeekBar.getValue() + "% confidence");
            mBirdSeekBar.setOnPreferenceChangeListener((preference, newValue) -> {
                mBirdSwitchTakePhoto.setSummaryOn("Photos of the birds will be taken if the machine detects with " + newValue + "% confidence");
                return true;
            });

            SwitchPreference mSquirrelSwitchTakePhoto = findPreference("squirrelSwitchTakePhoto");
            CheckBoxPreference mDefaultSquirrelTakePhotoCheckBox = findPreference("defaultSquirrelTakePhotoCheckBox");
            SeekBarPreference mSquirrelSeekBar = findPreference("squirrelSeekBar");

            assert mDefaultSquirrelTakePhotoCheckBox != null;
            mDefaultSquirrelTakePhotoCheckBox.setOnPreferenceChangeListener((preference, newValue) -> {
                if(mDefaultSquirrelTakePhotoCheckBox.isChecked()) {
                    assert mSquirrelSeekBar != null;
                    mSquirrelSeekBar.setValue(getDefaultConfidenceLevel());
                } else {
                    assert mSquirrelSwitchTakePhoto != null;
                    mSquirrelSwitchTakePhoto.setSummaryOn("Boxes will show up around detected animals if the machine detects at " + getDefaultConfidenceLevel() + "% confidence");
                }
                return true;
            });

            assert mSquirrelSeekBar != null;
            assert mSquirrelSwitchTakePhoto != null;
            mSquirrelSwitchTakePhoto.setSummaryOn("Photos of the squirrels will be taken if the machine detects with " + mSquirrelSeekBar.getValue() + "% confidence");
            mSquirrelSeekBar.setOnPreferenceChangeListener((preference, newValue) -> {
                mSquirrelSwitchTakePhoto.setSummaryOn("Photos of the squirrels will be taken if the machine detects with " + newValue + "% confidence");
                return true;
            });
        }
    }
}