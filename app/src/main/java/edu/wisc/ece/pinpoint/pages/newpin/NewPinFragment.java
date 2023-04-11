package edu.wisc.ece.pinpoint.pages.newpin;

import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import edu.wisc.ece.pinpoint.utils.ValidationUtils;

public class NewPinFragment extends Fragment {
    private static final String TAG = NewPinFragment.class.getName();
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
    private ImageView pinnies_logo_topbar;
    private ImageView pinnies_logo_button;
    private ProgressBar userPinniesProgressBar;
    private ProgressBar pinCostProgressBar;
    private Long pinnieCount;
    private Long pinCost;
    private Location pinLocation;

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
            if (hasFocus) {
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
                    if (pinCost != null) {
                        setPinCostUI();
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            });
        }
    }

    private void setPinnieCountUI() {
        userPinniesProgressBar.setVisibility(View.GONE);
        pinnies_logo_topbar.setVisibility(View.VISIBLE);
        topBarText.setText(String.format("%s %s", getString(R.string.new_pin_title_text),
                FormatUtils.humanReadablePinnies(pinnieCount)));
    }

    private void setPinCost() {
        LocationDriver.getInstance(requireContext()).getCurrentLocation(requireContext())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        pinLocation = task.getResult();
                        Log.d(TAG, String.format("Location %s", pinLocation.toString()));
                        FirebaseDriver.getInstance().calcPinCost(pinLocation)
                                .addOnCompleteListener(t -> {
                                    pinCost = t.getResult().longValue();
                                    Log.d(TAG, String.format("Pin Cost: %s", pinCost));
                                    if (pinnieCount != null) {
                                        setPinCostUI();
                                    }
                                });
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                });
    }

    private void setPinCostUI() {
        dropButton.setText(String.format("%s %s", getString(R.string.drop_pin_button_text),
                FormatUtils.humanReadablePinnies(pinCost)));
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
        String caption = captionInput.getText().toString();

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
        }

        String textContent = type == PinType.TEXT ? textContentInput.getText().toString() : null;
        Pin pin = new Pin(textContent, type, pinLocation, caption);

        firebase.dropPin(pin, pinCost).addOnFailureListener(e -> {
            Log.w(TAG, "Error adding pin document", e);
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            dropButton.setEnabled(true);
        }).addOnSuccessListener(pid -> {
            if (pin.getType() == PinType.IMAGE) {
                Log.d(TAG, pid);
                firebase.uploadPinImage(fragmentAdapter.getImageContentFragment().photo_uri, pid)
                        .addOnCompleteListener(uploadTask -> {
                            if (!uploadTask.isSuccessful()) {
                                // TODO: create cloud function to delete pin & restore currency
                                Toast.makeText(requireContext(),
                                                R.string.photo_upload_error_message,
                                                Toast.LENGTH_LONG)
                                        .show();
                                dropButton.setEnabled(true);
                                return;
                            }
                            Toast.makeText(requireContext(), R.string.drop_pin_text,
                                    Toast.LENGTH_LONG).show();
                            navController.popBackStack();
                            navController.navigate(NewPinFragmentDirections.pinView(pid));

                        });
            } else {
                Toast.makeText(requireContext(), R.string.drop_pin_text, Toast.LENGTH_LONG).show();
                navController.popBackStack();
                navController.navigate(NewPinFragmentDirections.pinView(pid));
            }
        });
    }
}
