package edu.wisc.ece.pinpoint.pages.newpin;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.Pin.PinType;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;
import edu.wisc.ece.pinpoint.utils.LocationDriver;
import edu.wisc.ece.pinpoint.utils.PlacesAPIDriver;
import edu.wisc.ece.pinpoint.utils.ValidationUtils;

public class NewPinFragment extends Fragment {
    private static final String TAG = NewPinFragment.class.getName();

    private static final int LOADING_LOCATIONS = -2;
    private static final int NO_LOCATIONS_FOUND = -1;
    private FirebaseDriver firebase;
    private LocationDriver locationDriver;
    private NestedScrollView scrollView;
    private NavController navController;
    private TextView topBarText;
    private TextView insufficientPinniesText;
    private NewPinFragmentAdapter fragmentAdapter;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private EditText captionInput;
    private Button dropButton;
    private EditText textContentInput;
    private EditText locationNameInput;
    private ImageView pinnies_logo_topbar;
    private ImageView pinnies_logo_button;
    private ProgressBar userPinniesProgressBar;
    private ProgressBar pinCostProgressBar;
    private ArrayAdapter<String> locationNameAdapter;
    private Long pinnieCount;
    private Long pinCost;
    private Location pinLocation;
    private int selectedLocationIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
        locationDriver = LocationDriver.getInstance(requireContext());
        selectedLocationIndex = LOADING_LOCATIONS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_pin, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        scrollView = requireView().findViewById(R.id.newpin_scrollview);
        captionInput = requireView().findViewById(R.id.newpin_caption_input);
        topBarText = requireView().findViewById(R.id.new_pin_title);
        insufficientPinniesText = requireView().findViewById(R.id.pinnies_error_text);
        userPinniesProgressBar = requireView().findViewById(R.id.new_pin_balance_progress);
        pinCostProgressBar = requireView().findViewById(R.id.new_pin_cost_progress);
        ImageButton cancelButton = requireView().findViewById(R.id.newpin_cancel);
        cancelButton.setOnClickListener(v -> navController.popBackStack());
        dropButton = requireView().findViewById(R.id.drop_pin_button);
        dropButton.setOnClickListener(v -> createNewPin());
        tabLayout = requireView().findViewById(R.id.newpin_tab_layout);
        viewPager = requireView().findViewById(R.id.newpin_view_pager);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.text_text));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.image_text));
        pinnies_logo_topbar = requireView().findViewById(R.id.topbar_pinnies_logo);
        pinnies_logo_button = requireView().findViewById(R.id.button_pinnies_logo);
        locationNameInput = requireView().findViewById(R.id.newpin_location_name_input);
        Spinner locationNameSelect = requireView().findViewById(R.id.newpin_location_name_select);

        if (!locationDriver.hasFineLocation(requireContext())) {
            Toast.makeText(requireContext(), R.string.fine_location_error_text, Toast.LENGTH_LONG)
                    .show();
            navController.popBackStack();
            return;
        }

        // Setup location select input and select spinner
        locationNameInput.setText(R.string.loading_text);
        requireView().findViewById(R.id.newpin_location_name_input)
                .setOnClickListener(v -> locationNameSelect.performClick());
        locationNameAdapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        locationNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationNameSelect.setAdapter(locationNameAdapter);
        locationNameSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                locationNameInput.setText((String) adapterView.getItemAtPosition(i));
                selectedLocationIndex = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Required override method but does nothing
            }
        });

        fragmentAdapter =
                new NewPinFragmentAdapter(getChildFragmentManager(), tabLayout.getTabCount(),
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

        captionInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && Resources.getSystem()
                    .getDisplayMetrics().heightPixels == getView().getHeight()) {
                scrollView.postDelayed(() -> scrollView.scrollTo(0,
                        Resources.getSystem().getDisplayMetrics().heightPixels), 100);
            } else {
                scrollView.post(() -> scrollView.scrollTo(0, 0));
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

        setPinnieCount();
        setPinCost();
        getNearbyLocations();
    }

    @SuppressLint("MissingPermission")
    private void getNearbyLocations() {
        PlacesAPIDriver.getInstance(requireContext()).getNearbyPlaces().addOnFailureListener(e -> {
            Log.w(TAG, e);
            locationNameInput.setText(R.string.nearby_place_fetch_error_message_short);
            if (getContext() != null)
                Toast.makeText(getContext(), R.string.nearby_place_fetch_error_message,
                        Toast.LENGTH_LONG).show();
        }).addOnSuccessListener(places -> {
            if (places.size() > 0) {
                locationNameAdapter.addAll(places);
                selectedLocationIndex = 0;
            } else {
                locationNameInput.setText(R.string.no_nearby_locations_text);
                selectedLocationIndex = NO_LOCATIONS_FOUND;
            }
        });
    }

    private void setPinnieCount() {
        if (firebase.getCachedPinnies() != null) {
            pinnieCount = firebase.getCachedPinnies();
            setPinnieCountUI();
        } else {
            firebase.getPinnies().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    pinnieCount = task.getResult();
                    Log.d(TAG, String.format("Got %s pinnies for user", pinnieCount.toString()));
                    setPinnieCountUI();
                    if (pinCost != null && getContext() != null) {
                        setPinCostUI();
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                    if (getContext() != null)
                        Toast.makeText(getContext(), R.string.pinnie_fetch_error_message,
                                Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setPinnieCountUI() {
        userPinniesProgressBar.setVisibility(View.GONE);
        pinnies_logo_topbar.setVisibility(View.VISIBLE);
        topBarText.setText(String.format("%s %s", getString(R.string.new_pin_title_text),
                FormatUtils.trimmedNumber(pinnieCount)));
    }

    private void setPinCost() {
        LocationDriver.getInstance(requireContext()).getCurrentLocation(requireContext())
                .addOnCompleteListener(locationTask -> {
                    if (locationTask.isSuccessful() && locationTask.getResult() != null) {
                        pinLocation = locationTask.getResult();
                        Log.d(TAG, String.format("Location %s", pinLocation.toString()));
                        FirebaseDriver.getInstance().calcPinCost(pinLocation)
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.d(TAG, "get failed with ", task.getException());
                                        if (getContext() != null) Toast.makeText(getContext(),
                                                R.string.pin_cost_calc_error_message,
                                                Toast.LENGTH_LONG).show();
                                    }
                                    pinCost = task.getResult().longValue();
                                    Log.d(TAG, String.format("Pin Cost: %s", pinCost));
                                    if (pinnieCount != null && getContext() != null) {
                                        setPinCostUI();
                                    }
                                });
                    } else {
                        Log.d(TAG, "get failed with ", locationTask.getException());
                        if (getContext() != null)
                            Toast.makeText(getContext(), R.string.location_error_text,
                                    Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setPinCostUI() {
        dropButton.setText(String.format("%s %s", getString(R.string.drop_pin_button_text),
                FormatUtils.trimmedNumber(pinCost)));
        pinCostProgressBar.setVisibility(View.GONE);
        pinnies_logo_button.setVisibility(View.VISIBLE);

        if (pinnieCount >= pinCost) {
            dropButton.setEnabled(true);
        } else {
            insufficientPinniesText.setVisibility(View.VISIBLE);
        }
    }

    private void createNewPin() {
        dropButton.setEnabled(false);
        PinType type = viewPager.getCurrentItem() == 0 ? PinType.TEXT : PinType.IMAGE;
        String caption = captionInput.getText().toString().trim();

        if (type == PinType.TEXT) {
            textContentInput = requireView().findViewById(R.id.newpin_text_content_input);
            TextInputLayout textContentLayout =
                    requireView().findViewById(R.id.newpin_text_content_input_layout);
            if (ValidationUtils.isEmpty(textContentInput)) {
                textContentLayout.setError(getString(R.string.empty_pin_text));
                dropButton.setEnabled(true);
                return;
            } else {
                textContentLayout.setErrorEnabled(false);
            }
        } else {
            // type == IMAGE
            if (fragmentAdapter.getImageContentFragment().photo_uri == null) {
                Toast.makeText(requireContext(), R.string.empty_pin_text, Toast.LENGTH_SHORT)
                        .show();
                dropButton.setEnabled(true);
                return;
            }
        }

        if (pinLocation == null) {
            Toast.makeText(requireContext(), R.string.location_error_text, Toast.LENGTH_LONG)
                    .show();
            dropButton.setEnabled(true);
            return;
        } else if (!locationDriver.hasFineLocation(requireContext())) {
            Toast.makeText(requireContext(), R.string.fine_location_error_text, Toast.LENGTH_LONG)
                    .show();
            dropButton.setEnabled(true);
            return;
        } else if (selectedLocationIndex == LOADING_LOCATIONS || ValidationUtils.isEmpty(
                locationNameInput)) {
            Toast.makeText(requireContext(), R.string.nearby_location_name_missing_message,
                    Toast.LENGTH_LONG).show();
            dropButton.setEnabled(true);
            return;
        }

        String textContent = type == PinType.TEXT ? textContentInput.getText().toString() : null;
        String broadLocationName =
                locationNameAdapter.getCount() > 0 ? locationNameAdapter.getItem(0) : null;
        String nearbyLocationName =
                locationNameAdapter.getCount() > 0 && selectedLocationIndex > 0 ?
                        locationNameAdapter.getItem(selectedLocationIndex) : null;
        Pin pin = new Pin(textContent, type, pinLocation, caption, broadLocationName,
                nearbyLocationName);

        firebase.dropPin(pin, pinCost).addOnFailureListener(e -> {
            Log.w(TAG, "Error adding pin document", e);
            if (getContext() != null)
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            dropButton.setEnabled(true);
        }).addOnSuccessListener(pid -> {
            if (pin.getType() == PinType.IMAGE) {
                Log.d(TAG, pid);
                firebase.uploadPinImage(fragmentAdapter.getImageContentFragment().photo_uri, pid)
                        .addOnCompleteListener(uploadTask -> {
                            if (!uploadTask.isSuccessful()) {
                                // TODO: create cloud function to delete pin & restore currency
                                if (getContext() != null) Toast.makeText(getContext(),
                                                R.string.photo_upload_error_message,
                                                Toast.LENGTH_LONG)
                                        .show();
                                dropButton.setEnabled(true);
                                return;
                            }

                            if (getContext() != null)
                                Toast.makeText(getContext(), R.string.drop_pin_text,
                                        Toast.LENGTH_LONG).show();

                            navController.popBackStack();
                            navController.navigate(NewPinFragmentDirections.pinView(pid));

                        });
            } else {
                if (getContext() != null)
                    Toast.makeText(getContext(), R.string.drop_pin_text, Toast.LENGTH_LONG).show();
                navController.popBackStack();
                navController.navigate(NewPinFragmentDirections.pinView(pid));
            }
        });
    }
}
