package edu.wisc.ece.pinpoint.pages.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import edu.wisc.ece.pinpoint.AuthActivity;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        Preference logOut = getPreferenceManager().findPreference("logout");
        if (logOut != null) {
            logOut.setOnPreferenceClickListener(preference -> {
                FirebaseDriver.getInstance().logout(requireContext())
                        .addOnCompleteListener(task -> {
                            Intent intent = new Intent(requireContext(), AuthActivity.class);
                            startActivity(intent);
                            requireActivity().finish();
                        });
                return true;
            });
        }
        Preference deleteAccount = getPreferenceManager().findPreference("deleteAccount");
        if (deleteAccount != null) {
            deleteAccount.setOnPreferenceClickListener(preference -> {
                FirebaseDriver.getInstance().logout(requireContext())
                        .addOnCompleteListener(task -> {
                            Intent intent = new Intent(requireContext(), AuthActivity.class);
                            startActivity(intent);
                            requireActivity().finish();
                        });
                return true;
            });
        }
    }


}