package edu.wisc.ece.pinpoint.pages.profile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuInflater;
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
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

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
    private ActivityResultLauncher<Intent> launcher;
    private MenuInflater menuInflater;
    private Uri photo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
        // create the launcher that will obtain and upload image
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                photo = result.getData().getData();
                Glide.with(requireContext()).load(photo).circleCrop().into(profilePicUpload);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @SuppressLint("RestrictedApi")
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
        profilePicUpload.setOnClickListener(this::uploadPicture);
        User cachedUser = firebase.getCachedUser(firebase.getCurrentUser().getUid());
        if (cachedUser != null) {
            usernameInput.setText(cachedUser.getUsername());
            locationInput.setText(cachedUser.getLocation());
            bioInput.setText(cachedUser.getBio());
            cachedUser.loadProfilePic(profilePicUpload, this);
        }
    }



    private void uploadPicture(View view) {
        // create gallery-opening intent
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // launch intent
        launcher.launch(intent);

    }

    private void updatePicture(Uri uri, User cachedUser, String uid) {
        // Method must exist to execute actions without timing out
        cachedUser.setProfilePicUrl(uri.toString());
        cachedUser.save(uid);
        Toast.makeText(requireActivity(),"Image successfully uploaded.", Toast.LENGTH_SHORT).show();
        navController.navigate(EditProfileFragmentDirections.profile());
    }

    public void save(View buttonView) {
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
            String newLocation = locationInput.getText().toString().trim().equals("") ? null : locationInput.getText().toString().trim();
            String newBio = bioInput.getText().toString().trim().equals("") ? null : bioInput.getText().toString().trim();
            if (photo != null) {
                // base storage reference created
                StorageReference baseRef = FirebaseStorage.getInstance().getReference();
                // profile image reference
                StorageReference pictureRef = baseRef.child("users/"+uid);
                // upload photo
                UploadTask uploadTask = pictureRef.putFile(photo);
                uploadTask.addOnFailureListener((exception) -> {
                    // Handle unsuccessful uploads
                    Toast.makeText(requireActivity(),"Image upload failed.", Toast.LENGTH_SHORT).show();
                }).addOnSuccessListener(taskSnapshot -> pictureRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updatePicture(uri, cachedUser, uid);
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
                }));
            }
            else {
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
}