package edu.wisc.ece.pinpoint.pages.profile;

import android.os.Bundle;

import edu.wisc.ece.pinpoint.R;
import androidx.preference.PreferenceFragmentCompat;
public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }
}