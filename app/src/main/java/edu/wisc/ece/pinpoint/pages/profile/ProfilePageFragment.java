package edu.wisc.ece.pinpoint.pages.profile;

import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class ProfilePageFragment extends Fragment {
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
    private TextView bio;
    private ImageView profilePic;
    private Button button;

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
        followingCount = requireView().findViewById(R.id.profile_following_count);
        pinsDroppedCount = requireView().findViewById(R.id.profile_dropped_count);
        pinsFoundCount = requireView().findViewById(R.id.profile_found_count);
        location = requireView().findViewById(R.id.profile_location);
        bio = requireView().findViewById(R.id.profile_bio);
        profilePic = requireView().findViewById(R.id.profile_pic);
        button = requireView().findViewById(R.id.profile_button);

        Bundle args = getArguments();
        String uid = args != null ? ProfilePageFragmentArgs.fromBundle(args).getUid() : null;

        // If UID null, navigated from navbar, show settings button & set default UID
        if (uid == null) {
            uid = firebase.getCurrentUser().getUid();
            ImageButton settingsButton = requireView().findViewById(R.id.profile_settings);
            settingsButton.setVisibility(View.VISIBLE);
            settingsButton.setOnClickListener(clickedView -> navController.navigate(
                    ProfilePageFragmentDirections.settingsContainer()));
        } else {
            // Got here some other way than navbar, don't show settings and show back button instead
            ImageButton backButton = requireView().findViewById(R.id.profile_back_button);
            backButton.setVisibility(View.VISIBLE);
            backButton.setOnClickListener(v -> navController.popBackStack());
        }
        if (uid.equals(firebase.getCurrentUser().getUid())) {
            // Viewing own profile, button is for editing profile
            button.setText(R.string.edit_profile_text);
            button.setOnClickListener((buttonView) -> navController.navigate(
                    ProfilePageFragmentDirections.editProfile()));
        } else {
            // Fetch updated pin metadata in case changed since last view
            firebase.fetchUserPins(uid);
            // Configure button based on follow status
            setButton(uid);
        }

        User cachedUser = firebase.getCachedUser(uid);
        if (cachedUser != null) {
            setUserData(cachedUser, uid);
        }
        String finalUid = uid;
        firebase.fetchUser(uid).addOnCompleteListener(task -> setUserData(task.getResult(), finalUid));

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
        if (uid.equals(firebase.getCurrentUser().getUid())) {
            // if the profile belongs to the app user, display the cached values
            followerCount.setText(String.valueOf(firebase.getCachedFollowerIds().size()));
            followingCount.setText(String.valueOf(firebase.getCachedFollowingIds().size()));
        }
        else {
            followerCount.setText(String.valueOf(user.getNumFollowers()));
            followingCount.setText(String.valueOf(user.getNumFollowing()));
            pinsDroppedCount.setText(String.valueOf(user.getNumPinsDropped()));
            pinsFoundCount.setText(String.valueOf(user.getNumPinsFound()));
        }
        location.setText(user.getLocation());
        bio.setText(user.getBio());
        if (user.getLocation() == null) {
            location.setVisibility(View.GONE);
        } else {
            location.setVisibility(View.VISIBLE);
        }
        if (user.getBio() == null) {
            bio.setVisibility(View.GONE);
        } else {
            bio.setVisibility(View.VISIBLE);
        }
    }

    public void setButton(String user) {
        // Viewing someone else's profile, button is for following
        final String uid = user;
        // if the user is already following this profile, show unfollow button
        if (firebase.getCachedFollowingIds().containsKey(uid)) {
            button.setText(R.string.unfollow_text);
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.soft_red));
            button.setOnClickListener((buttonView) -> {
                firebase.unfollowUser(uid);
                setButton(user);
            });
        }
        else {
            // if this profile follows the user but the user does not, show follow back button
            if (firebase.getCachedFollowerIds().containsKey(uid))
                button.setText(R.string.follow_back_text);
            else button.setText(R.string.follow_text);
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue));
            button.setOnClickListener((buttonView) -> {
                firebase.followUser(uid);
                setButton(user);
            });
        }
    }
}