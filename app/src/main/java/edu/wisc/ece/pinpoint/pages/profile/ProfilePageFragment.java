package edu.wisc.ece.pinpoint.pages.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class ProfilePageFragment extends Fragment {
    private FirebaseDriver firebase;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TextView username;
    private TextView followerCount;
    private TextView followingCount;
    private TextView pinsDroppedCount;
    private TextView pinsFoundCount;
    private TextView location;
    private TextView bio;
    private ShapeableImageView profilePic;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        username = requireView().findViewById(R.id.profile_username);
        followerCount = requireView().findViewById(R.id.profile_follower_count);
        followingCount = requireView().findViewById(R.id.profile_following_count);
        pinsDroppedCount = requireView().findViewById(R.id.profile_dropped_count);
        pinsFoundCount = requireView().findViewById(R.id.profile_found_count);
        location = requireView().findViewById(R.id.profile_location);
        bio = requireView().findViewById(R.id.profile_bio);

        if (savedInstanceState != null) {
            Log.d("TEST", "Loading saved instance state...");
            username.setText(savedInstanceState.getCharSequence("username"));
            followerCount.setText(savedInstanceState.getCharSequence("followerCount"));
            followingCount.setText(savedInstanceState.getCharSequence("followingCount"));
            pinsDroppedCount.setText(savedInstanceState.getCharSequence("pinsDroppedCount"));
            pinsFoundCount.setText(savedInstanceState.getCharSequence("pinsFoundCount"));
            location.setText(savedInstanceState.getCharSequence("location"));
            bio.setText(savedInstanceState.getCharSequence("bio"));
        }

        Bundle args = getArguments();
        String uid =
                args != null ? ProfilePageFragmentArgs.fromBundle(getArguments()).getUid() : null;
        if (uid == null) {
            requireView().findViewById(R.id.profile_follow).setVisibility(View.INVISIBLE);
            requireView().findViewById(R.id.profile_edit).setVisibility(View.VISIBLE);
            uid = firebase.getUser().getUid();
        }
        firebase.fetchUser(uid).addOnCompleteListener(task -> {
            User user = task.getResult();
            Log.d("TEST", "Updating data with firebase data...");
            username.setText(user.getUsername());
            followerCount.setText(String.valueOf(user.getNumFollowers()));
            followingCount.setText(String.valueOf(user.getNumFollowing()));
            pinsDroppedCount.setText(String.valueOf(user.getNumPinsDropped()));
            pinsFoundCount.setText(String.valueOf(user.getNumPinsFound()));
            location.setText(user.getLocation());
            bio.setText(user.getBio());
        });
        tabLayout = requireView().findViewById(R.id.tab_layout);
        viewPager = requireView().findViewById(R.id.view_pager);
        tabLayout.addTab(tabLayout.newTab().setText("Activity"));
        tabLayout.addTab(tabLayout.newTab().setText("Dropped Pins"));
        ProfileFragmentAdapter fragmentAdapter =
                new ProfileFragmentAdapter(requireActivity().getSupportFragmentManager(),
                        tabLayout.getTabCount(), getLifecycle());
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        // Not sure if saving state will work correctly if we have multiple user profiles
        super.onSaveInstanceState(state);
        state.putCharSequence("username", username.getText());
        state.putCharSequence("followerCount", followerCount.getText());
        state.putCharSequence("followingCount", followingCount.getText());
        state.putCharSequence("pinsDroppedCount", pinsDroppedCount.getText());
        state.putCharSequence("pinsFoundCount", pinsFoundCount.getText());
        state.putCharSequence("location", location.getText());
        state.putCharSequence("bio", bio.getText());
    }
}