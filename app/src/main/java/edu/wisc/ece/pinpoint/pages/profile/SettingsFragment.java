package edu.wisc.ece.pinpoint.pages.profile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
                FirebaseDriver.getInstance().logout(requireContext()).addOnSuccessListener(task -> {
                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                }).addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Something went wrong logging out. Please try again later.",
                        Toast.LENGTH_LONG).show());
                return true;
            });
        }
        Preference deleteAccount = getPreferenceManager().findPreference("deleteAccount");
        if (deleteAccount != null) {
            deleteAccount.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext());
                dialog.setTitle("Confirm Account Deletion");
                dialog.setMessage(R.string.account_delete_message);
                dialog.setPositiveButton(R.string.delete_text, this::deleteAccount);
                dialog.setNegativeButton(R.string.cancel_text, (d, buttonId) -> {
                    // Cancelled dialog
                });
                dialog.show();
                return true;
            });
        }
    }

    private void deleteAccount(DialogInterface dialog, int buttonId) {
        FirebaseDriver.getInstance().deleteAccount(requireContext()).addOnFailureListener(
                e -> Toast.makeText(getContext(),
                        "Something went wrong deleting your account. Please try again later.",
                        Toast.LENGTH_LONG).show()).addOnSuccessListener(t -> {
            Toast.makeText(getContext(), "Your account has been deleted.", Toast.LENGTH_LONG)
                    .show();
            if (getContext() != null && getActivity() != null) {
                Intent intent = new Intent(getContext(), AuthActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }
}