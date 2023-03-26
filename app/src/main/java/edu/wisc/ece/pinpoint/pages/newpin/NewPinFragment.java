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
import com.google.firebase.auth.FirebaseUser;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.Pin.PinType;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class NewPinFragment extends Fragment {
    private static final String TAG = NewPinFragment.class.getName();
    private FirebaseDriver firebase;
    private NavController navController;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private EditText textContentInput;
    private EditText captionInput;
    private Button dropButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_pin, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebase = FirebaseDriver.getInstance();
        navController = Navigation.findNavController(view);

        captionInput = requireView().findViewById(R.id.newpin_caption_input);
        textContentInput = requireView().findViewById(R.id.newpin_text_content_input);

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
        String content = String.valueOf(textContentInput.getText());
        String caption = String.valueOf(captionInput.getText());
        FirebaseUser user = firebase.getCurrentUser();
        // TODO: add input validation (prevent empty pins, etc)
        // TODO: determine and set PinType based on currently selected tab fragment
        Pin p = new Pin(caption, user.getUid(), PinType.TEXT, content);
        p.save().addOnSuccessListener(documentReference -> {
            Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
            Toast.makeText(requireContext(), "Successfully dropped Pin!", Toast.LENGTH_SHORT)
                    .show();
            // TODO: navigate user to pin view page
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Error adding document", e);
            Toast.makeText(requireContext(), "Error dropping Pin...", Toast.LENGTH_SHORT).show();
            dropButton.setEnabled(true);
        });
    }
}