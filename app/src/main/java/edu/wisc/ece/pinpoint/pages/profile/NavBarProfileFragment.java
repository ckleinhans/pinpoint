package edu.wisc.ece.pinpoint.pages.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class NavBarProfileFragment extends Fragment {
    private FirebaseDriver firebase;
    private NavController navController;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TextView username;
    private TextView followerCount;
    private TextView followingCount;
    private TextView pinsDroppedCount;
    private TextView pinsFoundCount;
    private TextView location;
    private ConstraintLayout locationLayout;
    private TextView bio;
    private ImageView profilePic;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        username = requireView().findViewById(R.id.profile_username);
        followerCount = requireView().findViewById(R.id.profile_follower_count);
        ConstraintLayout followerLayout = requireView().findViewById(R.id.profile_follower_layout);
        followingCount = requireView().findViewById(R.id.profile_following_count);
        ConstraintLayout followingLayout =
                requireView().findViewById(R.id.profile_following_layout);
        pinsDroppedCount = requireView().findViewById(R.id.profile_dropped_count);
        pinsFoundCount = requireView().findViewById(R.id.profile_found_count);
        location = requireView().findViewById(R.id.profile_location);
        locationLayout = requireView().findViewById(R.id.profile_location_layout);
        bio = requireView().findViewById(R.id.profile_bio);
        profilePic = requireView().findViewById(R.id.profile_pic);
        Button button = requireView().findViewById(R.id.profile_button);

        String uid = firebase.getUid();
        ImageButton settingsButton = requireView().findViewById(R.id.profile_settings);
        settingsButton.setVisibility(View.VISIBLE);
        settingsButton.setOnClickListener(clickedView -> navController.navigate(
                NavBarProfileFragmentDirections.settingsContainer()));

        // Viewing own profile, button is for editing profile
        button.setText(R.string.edit_profile_text);
        button.setOnClickListener((buttonView) -> navController.navigate(
                NavBarProfileFragmentDirections.editProfile()));

        User cachedUser = firebase.getCachedUser(uid);
        if (cachedUser != null) {
            setUserData(cachedUser, uid);
        }
        firebase.fetchUser(uid).addOnCompleteListener(task -> setUserData(task.getResult(), uid));

        followerLayout.setOnClickListener(v -> navController.navigate(
                ProfilePageFragmentDirections.userList(UserListFragment.UserListType.FOLLOWERS,
                        uid)));
        followingLayout.setOnClickListener(v -> navController.navigate(
                ProfilePageFragmentDirections.userList(UserListFragment.UserListType.FOLLOWING,
                        uid)));

        tabLayout = requireView().findViewById(R.id.tab_layout);
        viewPager = requireView().findViewById(R.id.view_pager);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.activity_text));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.dropped_pins_text));
        ProfileFragmentAdapter fragmentAdapter =
                new ProfileFragmentAdapter(getChildFragmentManager(), tabLayout.getTabCount(),
                        getLifecycle(), uid);
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

    public void setUserData(@NonNull User user, String uid) {
        user.loadProfilePic(profilePic, this);
        username.setText(user.getUsername());
        followerCount.setText(String.valueOf(user.getNumFollowers()));
        followingCount.setText(String.valueOf(user.getNumFollowing()));
        pinsDroppedCount.setText(String.valueOf(user.getNumPinsDropped()));
        pinsFoundCount.setText(String.valueOf(user.getNumPinsFound()));
        location.setText(user.getLocation());
        bio.setText(user.getBio());
        if (user.getLocation() == null) {
            locationLayout.setVisibility(View.GONE);
        } else {
            locationLayout.setVisibility(View.VISIBLE);
        }
        if (user.getBio() == null) {
            bio.setVisibility(View.GONE);
        } else {
            bio.setVisibility(View.VISIBLE);
        }
    }
}