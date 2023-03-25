package edu.wisc.ece.pinpoint.pages.profile;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.PlacesAPIDriver;
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
    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private ActivityResultLauncher<Intent> locationAutocompleteLauncher;
    private Uri photo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
        // Required to initialize places API
        PlacesAPIDriver.getInstance(requireContext());
        photoPickerLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                photo = result.getData().getData();
                                Glide.with(requireContext()).load(photo).circleCrop()
                                        .into(profilePicUpload);
                            }
                        });
        locationAutocompleteLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            int resultCode = result.getResultCode();
                            Intent data = result.getData();
                            if (resultCode == RESULT_OK && data != null) {
                                locationInputLayout.setErrorEnabled(false);
                                String address = Autocomplete.getPlaceFromIntent(data).getAddress();
                                if (address != null) {
                                    address = address.replaceAll("[, ]?\\d{5}", "");
                                }
                                locationInput.setText(address);
                                bioInput.requestFocus();
                            } else if (resultCode != RESULT_CANCELED) {
                                locationInputLayout.setError(
                                        getString(R.string.location_error_text));
                            }
                        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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

        // Make location input open autocomplete picker instead of allowing typing
        locationInput.setKeyListener(null);
        locationInput.setOnFocusChangeListener(this::launchLocationAutocomplete);
        locationInput.setOnClickListener(
                (locationView) -> launchLocationAutocomplete(locationView, true));

        cancelButton.setOnClickListener(
                (buttonView) -> navController.navigate((NavDirections) EditProfileFragmentDirections.profile()));
        saveButton.setOnClickListener(this::save);
        profilePicUpload.setOnClickListener(this::uploadPicture);

        User cachedUser = firebase.getCachedUser(firebase.getCurrentUser().getUid());
        if (cachedUser != null) {
            usernameInput.setText(cachedUser.getUsername());
            locationInput.setText(cachedUser.getLocation());
            bioInput.setText(cachedUser.getBio());
            cachedUser.loadProfilePic(profilePicUpload, this);
        }
    }

    private void save(View buttonView) {
        if (ValidationUtils.isEmpty(usernameInput)) {
            usernameInputLayout.setError(getString(R.string.missing_username));
            return;
        } else {
            usernameInputLayout.setErrorEnabled(false);
        }

        buttonView.setEnabled(false);
        String uid = firebase.getCurrentUser().getUid();
        User cachedUser = firebase.getCachedUser(uid);

        String oldUsername = cachedUser.getUsername();
        String oldLocation = cachedUser.getLocation();
        String oldBio = cachedUser.getBio();
        String oldProfilePicUrl = cachedUser.getProfilePicUrl();

        String newUsername = usernameInput.getText().toString().trim();
        String newLocation = locationInput.getText().toString().trim().equals("") ? null :
                locationInput.getText().toString().trim();
        String newBio = bioInput.getText().toString().trim().equals("") ? null :
                bioInput.getText().toString().trim();

        OnCompleteListener<Void> saveUserDataListener = task -> {
            if (task.isSuccessful()) {
                navController.navigate((NavDirections) EditProfileFragmentDirections.profile());
            } else {
                cachedUser.setUsername(oldUsername).setLocation(oldLocation).setBio(oldBio)
                        .setProfilePicUrl(oldProfilePicUrl, false);
                Toast.makeText(requireContext(), "Couldn't save profile. Try again later.",
                        Toast.LENGTH_LONG).show();
                buttonView.setEnabled(true);
            }
        };

        if (photo != null) {
            StorageReference pictureRef =
                    FirebaseStorage.getInstance().getReference().child("users/" + uid);
            pictureRef.putFile(photo).addOnCompleteListener((task) -> {
                if (task.isSuccessful()) {
                    pictureRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        cachedUser.setUsername(newUsername).setLocation(newLocation).setBio(newBio)
                                .setProfilePicUrl(uri.toString());
                        cachedUser.save(uid).addOnCompleteListener(saveUserDataListener);
                    });
                } else {
                    Toast.makeText(requireActivity(), "Image upload failed. Try again later.",
                            Toast.LENGTH_LONG).show();
                    buttonView.setEnabled(true);
                }
            });
        } else {
            cachedUser.setUsername(newUsername).setLocation(newLocation).setBio(newBio);
            cachedUser.save(uid).addOnCompleteListener(saveUserDataListener);
        }
    }

    private void uploadPicture(View view) {
        // launch gallery opening intent
        photoPickerLauncher.launch(
                new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    private void launchLocationAutocomplete(View view, boolean isFocused) {
        if (isFocused) {
            List<String> placesFilter = Arrays.asList("locality", "sublocality", "colloquial_area",
                    "administrative_area_level_2", "administrative_area_level_3");
            List<Place.Field> requestedFields = Collections.singletonList(Place.Field.ADDRESS);
            locationAutocompleteLauncher.launch(
                    new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,
                            requestedFields).setTypesFilter(placesFilter).build(requireContext()));
        }
    }
}