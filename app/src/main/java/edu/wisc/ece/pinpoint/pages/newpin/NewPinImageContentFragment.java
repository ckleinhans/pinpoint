package edu.wisc.ece.pinpoint.pages.newpin;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import edu.wisc.ece.pinpoint.R;

public class NewPinImageContentFragment extends Fragment {

    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private Uri photo;

    private ImageView imageContentUpload;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_pin_image_content, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoPickerLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                photo = result.getData().getData();
                                Glide.with(requireContext()).load(photo).centerCrop()
                                        .into(imageContentUpload);
                            }
                        });
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageContentUpload = requireView().findViewById(R.id.newpin_addimageicon);

        imageContentUpload.setOnClickListener(this::uploadPicture);

    }

    private void uploadPicture(View view) {
        // launch gallery opening intent
        photoPickerLauncher.launch(
                new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }
}