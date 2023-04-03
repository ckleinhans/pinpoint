package edu.wisc.ece.pinpoint.pages.newpin;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

    private Integer userPinniesCount;
    private Integer pinCost;

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

        ImageButton cancelButton = requireView().findViewById(R.id.newpin_cancel);
        cancelButton.setOnClickListener(v -> navController.popBackStack());
        dropButton = requireView().findViewById(R.id.drop_pin_button);
        dropButton.setOnClickListener(v -> createNewPin());

        tabLayout = requireView().findViewById(R.id.newpin_tab_layout);
        viewPager = requireView().findViewById(R.id.newpin_view_pager);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.text_text));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.image_text));
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

        userPinniesCount = 1234;
        pinCost = 1;

        if(userPinniesCount < pinCost){
            dropButton.setEnabled(false);
            insufficientPinniesText.setVisibility(view.VISIBLE);
        }

        topBarText.setText("Balance: " + pinniesToString(userPinniesCount));
        dropButton.setText("Drop Pin - " + pinniesToString(pinCost));

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

        locationDriver.getCurrentLocation(requireContext()).addOnCompleteListener(locationTask -> {
            if (!locationTask.isSuccessful() || locationTask.getResult() == null) {
                Toast.makeText(requireContext(), R.string.location_error_text, Toast.LENGTH_LONG)
                        .show();
                dropButton.setEnabled(true);
                return;
            } else if (!locationDriver.hasFineLocation(requireContext())) {
                Toast.makeText(requireContext(), R.string.fine_location_error_text,
                        Toast.LENGTH_LONG).show();
                dropButton.setEnabled(true);
                return;
            }

            String textContent =
                    type == PinType.TEXT ? textContentInput.getText().toString() : null;
            Pin pin = new Pin(textContent, type, locationTask.getResult(), caption);

            firebase.dropPin(pin).addOnFailureListener(e -> {
                Log.w(TAG, "Error adding pin document", e);
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                dropButton.setEnabled(true);
            }).addOnSuccessListener(pid -> {
                if (pin.getType() == PinType.IMAGE) {
                    Log.d(TAG, pid);
                    firebase.uploadPinImage(fragmentAdapter.getImageContentFragment().photo_uri,
                            pid).addOnCompleteListener(uploadTask -> {
                        if (!uploadTask.isSuccessful()) {
                            // TODO: create cloud function to delete pin & restore currency
                            Toast.makeText(requireContext(), R.string.photo_upload_error_message,
                                    Toast.LENGTH_LONG).show();
                            dropButton.setEnabled(true);
                            return;
                        }
                        Toast.makeText(requireContext(), R.string.drop_pin_text, Toast.LENGTH_LONG)
                                .show();
                        navController.popBackStack();
                        navController.navigate(NewPinFragmentDirections.pinView(pid));

                    });
                } else {
                    Toast.makeText(requireContext(), R.string.drop_pin_text, Toast.LENGTH_LONG)
                            .show();
                    navController.popBackStack();
                    navController.navigate(NewPinFragmentDirections.pinView(pid));
                }
            });
        });
    }

    private void dropPin(@NonNull Pin pin) {

    }

    private String pinniesToString(Integer pinniesCount) {
        String tempString;

        if (pinniesCount < 999) {
            return pinniesCount.toString();
        } else if (pinniesCount < 999999) {
            tempString = pinniesCount/1000 + "." + (pinniesCount % 1000) / 100 + "" + (pinniesCount % 100) / 10 + "K";
        } else if (pinniesCount < 999999999) {
            tempString = pinniesCount / 1000000 + "." + (pinniesCount % 1000000) / 100000 + "" + (pinniesCount % 100000) / 10000 + "M";
        }
        else return pinniesCount.toString();

        if(tempString.charAt(3) == '.'){
            return tempString.substring(0, 3) + tempString.charAt(tempString.length() - 1);
        }
        else{
            return tempString.substring(0, 4) + tempString.charAt(tempString.length() - 1);
        }

    }
}
