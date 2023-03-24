package edu.wisc.ece.pinpoint.pages.newpin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.Pin.PinType;

public class NewPinFragment extends Fragment {

    private static final String TAG = Pin.class.getName();

    private TabLayout newpin_tabLayout;
    private ViewPager2 newpin_viewPager;

    private ScrollView newpin_scrollview;

    private EditText newpin_inputeditlayout;

    private Button dropButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_pin, container, false);
    }


    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        newpin_tabLayout = requireView().findViewById(R.id.newpin_tab_layout);
        newpin_viewPager = requireView().findViewById(R.id.newpin_view_pager);
        newpin_scrollview = requireView().findViewById(R.id.newpin_scrollview);
        newpin_inputeditlayout = requireView().findViewById(R.id.newpin_inputeditlayout);
        newpin_tabLayout.addTab(newpin_tabLayout.newTab().setText("Text"));
        newpin_tabLayout.addTab(newpin_tabLayout.newTab().setText("Image"));
        NewPinFragmentAdapter fragmentAdapter =
                new NewPinFragmentAdapter(this.getChildFragmentManager(),
                        newpin_tabLayout.getTabCount(), getLifecycle());
        newpin_viewPager.setAdapter(fragmentAdapter);

        dropButton = requireView().findViewById(R.id.drop_pin_button);
        dropButton.setOnClickListener(v -> createNewPin());

        //Force the screen to scroll to the bottom after the caption text input gets focus.
        //Post delay is required as the scroll does not do anything if the fragment hasn't been pushed
        //up first, because the base fragment is too small (no scroll needed)
        newpin_inputeditlayout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    newpin_scrollview.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    newpin_scrollview.scrollTo(0, newpin_scrollview.getHeight()*2);
                                }
                }, 100);
                }else {

                }
            }
        });

        newpin_tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                newpin_viewPager.setCurrentItem(tab.getPosition());
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

        newpin_viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                //noinspection ConstantConditions
                newpin_tabLayout.getTabAt(position).select();
            }
        });


    }

    private void createNewPin() {
        EditText contentContainer = requireView().findViewById(R.id.newpin_textcontent_editlayout);
        String content = String.valueOf(contentContainer.getText());
        String caption = String.valueOf(newpin_inputeditlayout.getText());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // TODO: determine and set PinType based on currently selected tab fragment
        Pin p = new Pin(caption, user.getUid(), PinType.TEXT, content);
        Task<DocumentReference> ret = p.save();
        ret.addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                Toast.makeText(getContext(), "Successfully dropped Pin!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error adding document", e);
                Toast.makeText(getContext(), "Error dropping Pin...", Toast.LENGTH_SHORT).show();
            }
        });
    }

}