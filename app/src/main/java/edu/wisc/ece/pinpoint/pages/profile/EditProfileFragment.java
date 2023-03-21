package edu.wisc.ece.pinpoint.pages.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputLayout;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.ValidationUtils;

public class EditProfileFragment extends Fragment {
    private FirebaseDriver firebase;
    private NavController navController;
    private TextInputLayout usernameInputLayout;
    private TextInputLayout locationInputLayout;
    private EditText usernameInput;
    private EditText locationInput;
    private EditText bioInput;
    private ImageView profilePicUpload;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        usernameInputLayout = requireView().findViewById(R.id.profile_edit_username_layout);
        locationInputLayout = requireView().findViewById(R.id.profile_edit_location_layout);
        usernameInput = requireView().findViewById(R.id.profile_edit_username);
        locationInput = requireView().findViewById(R.id.profile_edit_location);
        bioInput = requireView().findViewById(R.id.profile_edit_bio);
        profilePicUpload = requireView().findViewById(R.id.profile_edit_image);

        ImageButton cancelButton = requireView().findViewById(R.id.profile_edit_cancel);
        ImageButton saveButton = requireView().findViewById(R.id.profile_edit_save);

        cancelButton.setOnClickListener(
                (buttonView) -> navController.navigate(EditProfileFragmentDirections.profile()));
        saveButton.setOnClickListener(this::save);
        User cachedUser = firebase.getCachedUser(firebase.getCurrentUser().getUid());
        if (cachedUser != null) {
            usernameInput.setText(cachedUser.getUsername());
            locationInput.setText(cachedUser.getLocation());
            bioInput.setText(cachedUser.getBio());
            // TODO: don't think we need shaped image view since Glide can circle crop image for us
            cachedUser.loadProfilePic(profilePicUpload, this);
        }
    }

    private void save(View buttonView) {
        boolean isValid = true;
        if (ValidationUtils.isEmpty(usernameInput)) {
            usernameInputLayout.setError(getString(R.string.missing_username));
            isValid = false;
        } else {
            usernameInputLayout.setErrorEnabled(false);
        }
        // TODO: somehow validate location (implement required autocomplete)

        if (isValid) {
            buttonView.setEnabled(false);
            String uid = firebase.getCurrentUser().getUid();
            User cachedUser = firebase.getCachedUser(uid);

            String oldUsername = cachedUser.getUsername();
            String oldLocation = cachedUser.getLocation();
            String oldBio = cachedUser.getBio();

            String newUsername = usernameInput.getText().toString().trim();
            String newLocation = locationInput.getText().toString().trim();
            if (newLocation.equals("")) {
                newLocation = null;
            }
            String newBio = bioInput.getText().toString().trim();
            if (newBio.equals("")) {
                newBio = null;
            }
            cachedUser.setUsername(newUsername).setLocation(newLocation).setBio(newBio);
            cachedUser.save(uid).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    navController.navigate(EditProfileFragmentDirections.profile());
                } else {
                    cachedUser.setUsername(oldUsername);
                    cachedUser.setLocation(oldLocation);
                    cachedUser.setBio(oldBio);
                    Toast.makeText(requireContext(), "Couldn't save profile. Try again later.",
                            Toast.LENGTH_LONG).show();
                    buttonView.setEnabled(true);
                }
            });
        }
    }
}