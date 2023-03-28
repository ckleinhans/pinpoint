package edu.wisc.ece.pinpoint.pages.newpin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.Pin.PinType;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.LocationDriver;
import edu.wisc.ece.pinpoint.utils.ValidationUtils;

public class NewPinFragment extends Fragment {
    private static final String TAG = NewPinFragment.class.getName();
    private FirebaseDriver firebase;
    private LocationDriver locationDriver;
    private NavController navController;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private EditText captionInput;
    private Button dropButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
        locationDriver = LocationDriver.getInstance(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_pin, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        captionInput = requireView().findViewById(R.id.newpin_caption_input);

        ImageButton cancelButton = requireView().findViewById(R.id.newpin_cancel);
        cancelButton.setOnClickListener(
                v -> navController.navigate(NewPinFragmentDirections.map()));
        dropButton = requireView().findViewById(R.id.drop_pin_button);
        dropButton.setOnClickListener(v -> createNewPin());

        tabLayout = requireView().findViewById(R.id.newpin_tab_layout);
        viewPager = requireView().findViewById(R.id.newpin_view_pager);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.text_text));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.image_text));
        NewPinFragmentAdapter fragmentAdapter =
                new NewPinFragmentAdapter(this.getChildFragmentManager(), tabLayout.getTabCount(),
                        getLifecycle());
        viewPager.setAdapter(fragmentAdapter);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Mandatory override intentionally blank, will not implement onTabUnselected
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Mandatory override intentionally blank, will not implement onTabReselected
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                //noinspection ConstantConditions
                tabLayout.getTabAt(position).select();
            }
        });
    }

    private void createNewPin() {
        dropButton.setEnabled(false);
        int currentTabIndex = viewPager.getCurrentItem();

        String content;
        if (currentTabIndex == 0) {
            // Text content pin
            EditText textContentInput = requireView().findViewById(R.id.newpin_text_content_input);
            TextInputLayout textContentLayout =
                    requireView().findViewById(R.id.newpin_text_content_input_layout);
            if (ValidationUtils.isEmpty(textContentInput)) {
                textContentLayout.setError(getString(R.string.empty_pin_text));
                dropButton.setEnabled(true);
                return;
            } else {
                textContentLayout.setErrorEnabled(false);
            }
            content = String.valueOf(textContentInput.getText());
        } else {
            // TODO: get locally stored image URL for content
            content = "IMAGE PLACEHOLDER";
        }

        PinType type = currentTabIndex == 0 ? PinType.TEXT : PinType.IMAGE;
        String caption = String.valueOf(captionInput.getText());
        FirebaseUser user = firebase.getCurrentUser();

        locationDriver.getCurrentLocation(requireContext()).addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(requireContext(),
                        "Couldn't get your location. Check your location settings!",
                        Toast.LENGTH_LONG).show();
                return;
            }
            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            Pin p = new Pin(caption, user.getUid(), type, content, geoPoint);
            p.save().addOnSuccessListener(documentReference -> {
                Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                Toast.makeText(requireContext(), "Successfully dropped Pin!", Toast.LENGTH_SHORT)
                        .show();
                // TODO: navigate user to pin view page
            }).addOnFailureListener(e -> {
                Log.w(TAG, "Error adding document", e);
                Toast.makeText(requireContext(), "Error dropping Pin...", Toast.LENGTH_SHORT)
                        .show();
                dropButton.setEnabled(true);
            });
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Error getting user location", e);
            Toast.makeText(requireContext(), "Error getting location...", Toast.LENGTH_SHORT)
                    .show();
            dropButton.setEnabled(true);
        });
    }
}