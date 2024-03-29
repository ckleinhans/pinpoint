package edu.wisc.ece.pinpoint.pages.newpin;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.wisc.ece.pinpoint.BuildConfig;
import edu.wisc.ece.pinpoint.R;

public class NewPinImageContentFragment extends Fragment {

    public Uri photo_uri;
    private ActivityResultLauncher<Intent> photoTakerLauncher;
    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private Handler h;
    private Runnable takePictureRunnable;
    private Runnable uploadPictureRunnable;
    private ImageView imageContentUpload;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoPickerLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                photo_uri = result.getData().getData();
                                Glide.with(requireContext()).load(photo_uri).centerCrop()
                                        .into(imageContentUpload);
                            }
                        });

        photoTakerLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Glide.with(this).load(photo_uri).centerCrop()
                                        .into(imageContentUpload);
                            }
                        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_pin_image_content, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        h = new Handler(Looper.getMainLooper());
        takePictureRunnable = this::takePicture;
        uploadPictureRunnable = this::uploadPicture;
        imageContentUpload = requireView().findViewById(R.id.newpin_addimageicon);
        imageContentUpload.setOnClickListener(view1 -> showSelectDialog());
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
            photo_uri = FileProvider.getUriForFile(requireContext(),
                    BuildConfig.APPLICATION_ID + ".fileprovider", photoFile);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photo_uri);
        }

        // launch gallery opening intent
        photoTakerLauncher.launch(pictureIntent);
    }

    private void uploadPicture() {
        // launch gallery opening intent
        photoPickerLauncher.launch(
                new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    private File createImageFile() throws IOException {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
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