package edu.wisc.ece.pinpoint.pages.profile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import edu.wisc.ece.pinpoint.AuthActivity;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class SettingsFragment extends PreferenceFragmentCompat {

    private ConstraintLayout deleteLoadLayoutContainer;
    private ImageButton backButton;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        //noinspection ConstantConditions
        deleteLoadLayoutContainer = this.getParentFragment().requireView()
                .findViewById(R.id.delete_acct_load_layout_container);
        backButton = this.getParentFragment().requireView().findViewById(R.id.settings_cancel);
        deleteLoadLayoutContainer.setOnClickListener(v -> {
        });

        setPreferencesFromResource(R.xml.settings, rootKey);
        Preference logOut = getPreferenceManager().findPreference("logout");
        if (logOut != null) {
            logOut.setOnPreferenceClickListener(preference -> {
                FirebaseDriver.getInstance().logout(requireContext()).addOnSuccessListener(task -> {
                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                }).addOnFailureListener(
                        e -> Toast.makeText(getContext(), R.string.logout_error_message,
                                Toast.LENGTH_LONG).show());
                return true;
            });
        }
        Preference deleteAccount = getPreferenceManager().findPreference("deleteAccount");
        if (deleteAccount != null) {
            deleteAccount.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext());
                dialog.setTitle(R.string.confirm_account_deletion_text);
                dialog.setMessage(R.string.account_delete_message);
                dialog.setPositiveButton(R.string.delete_text, this::deleteAccount);
                dialog.setNegativeButton(R.string.cancel_text, (d, buttonId) -> {
                    // Cancelled dialog
                });
                dialog.show();
                return true;
            });
        }
        Preference feedback = getPreferenceManager().findPreference("feedback");
        if (feedback != null) {
            feedback.setOnPreferenceClickListener(preference -> {
                FirebaseDriver.getInstance().startFeedback();
                return true;
            });
        }
    }

    private void lockUI() {
        deleteLoadLayoutContainer.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.INVISIBLE);
    }

    private void restoreUI() {
        deleteLoadLayoutContainer.setVisibility(View.GONE);
        backButton.setVisibility(View.VISIBLE);
    }

    private void deleteAccount(DialogInterface dialog, int buttonId) {
        lockUI();
        FirebaseDriver.getInstance().deleteAccount(requireContext()).addOnFailureListener(e -> {
            Toast.makeText(getContext(), R.string.delete_account_error_message, Toast.LENGTH_LONG)
                    .show();
            restoreUI();
        }).addOnSuccessListener(t -> {
            Toast.makeText(getContext(), R.string.account_deleted_message, Toast.LENGTH_LONG)
                    .show();
            restoreUI();
            if (getContext() != null && getActivity() != null) {
                Intent intent = new Intent(getContext(), AuthActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }
}