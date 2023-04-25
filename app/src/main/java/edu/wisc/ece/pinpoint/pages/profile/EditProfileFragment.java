package edu.wisc.ece.pinpoint.pages.profile;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.wisc.ece.pinpoint.BuildConfig;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.PlacesAPIDriver;
import edu.wisc.ece.pinpoint.utils.ValidationUtils;

public class EditProfileFragment extends Fragment {
    private FirebaseDriver firebase;
    private ActivityResultLauncher<Intent> photoTakerLauncher;
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
    private Handler h;
    private ConstraintLayout loadLayoutContainer;
    private Runnable takePictureRunnable;
    private Runnable uploadPictureRunnable;
    private ImageButton cancelButton;
    private ImageButton saveButton;

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
        photoTakerLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Glide.with(this).load(photo).circleCrop().into(profilePicUpload);
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
        loadLayoutContainer = requireView().findViewById(R.id.edit_prof_load_layout_container);
        cancelButton = requireView().findViewById(R.id.profile_edit_cancel);
        saveButton = requireView().findViewById(R.id.profile_edit_save);
        h = new Handler(Looper.getMainLooper());
        takePictureRunnable = this::takePicture;
        uploadPictureRunnable = this::uploadPicture;

        // Make location input open autocomplete picker instead of allowing typing
        locationInput.setKeyListener(null);
        locationInput.setOnFocusChangeListener(this::launchLocationAutocomplete);
        locationInput.setOnClickListener(
                (locationView) -> launchLocationAutocomplete(locationView, true));
        loadLayoutContainer.setOnClickListener(v -> {});
        cancelButton.setOnClickListener((buttonView) -> navController.popBackStack());
        saveButton.setOnClickListener(this::save);
        profilePicUpload.setOnClickListener(view1 -> showSelectDialog());

        User cachedUser = firebase.getCachedUser(firebase.getUid());
        if (cachedUser != null) {
            usernameInput.setText(cachedUser.getUsername());
            locationInput.setText(cachedUser.getLocation());
            bioInput.setText(cachedUser.getBio());
            cachedUser.loadProfilePic(profilePicUpload, this);
        }

        bioInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (null != bioInput.getLayout() && bioInput.getLayout().getLineCount() > 3) {
                    bioInput.getText()
                            .delete(bioInput.getText().length() - 1, bioInput.getText().length());
                }
            }
        });
    }

    private void lockUI(){
        loadLayoutContainer.setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        saveButton.setVisibility(View.INVISIBLE);
        cancelButton.setVisibility(View.INVISIBLE);
    }

    private void restoreUI(){
        loadLayoutContainer.setVisibility(View.GONE);
        saveButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
    }

    private void save(View buttonView) {

        if (ValidationUtils.isEmpty(usernameInput)) {
            usernameInputLayout.setError(getString(R.string.missing_username));
            return;
        } else {
            usernameInputLayout.setErrorEnabled(false);
        }

        lockUI();

        String uid = firebase.getUid();
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
                navController.popBackStack();
            } else {
                cachedUser.setUsername(oldUsername).setLocation(oldLocation).setBio(oldBio)
                        .setProfilePicUrl(oldProfilePicUrl, false);
                Toast.makeText(requireContext(), "Couldn't save profile. Try again later.",
                        Toast.LENGTH_LONG).show();
                restoreUI();
            }
        };

        if (photo != null) {
            StorageReference pictureRef =
                    FirebaseStorage.getInstance().getReference("users").child(uid);
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
                    restoreUI();
                }
            });
        } else {
            cachedUser.setUsername(newUsername).setLocation(newLocation).setBio(newBio);
            cachedUser.save(uid).addOnCompleteListener(saveUserDataListener);
        }
    }

    private void uploadPicture() {
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

    private void takePicture() {

        File photoFile;
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (pictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            //Create a file to store the image
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast toast = Toast.makeText(requireContext(),
                        getString(R.string.file_creation_failed_message), Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            photo = FileProvider.getUriForFile(requireContext(),
                    BuildConfig.APPLICATION_ID + ".fileprovider", photoFile);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photo);
        }

        // launch gallery opening intent
        photoTakerLauncher.launch(pictureIntent);
    }

    private File createImageFile() throws IOException {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */);

    }

    private void showSelectDialog() {
        new AlertDialog.Builder(requireContext()).setTitle(R.string.choose_image_title)
                .setMessage(R.string.choose_image_dialog_message)
                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(R.string.camera, (dialog, which) -> h.post(takePictureRunnable))
                // A null listener allows the button to dismiss the dialog and take no further
                // action.
                .setNegativeButton(R.string.gallery,
                        (dialog, which) -> h.post(uploadPictureRunnable)).show();
    }
}