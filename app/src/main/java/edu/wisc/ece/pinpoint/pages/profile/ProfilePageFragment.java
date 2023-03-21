package edu.wisc.ece.pinpoint.pages.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
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
        navController = Navigation.findNavController(view);
        username = requireView().findViewById(R.id.profile_username);
        followerCount = requireView().findViewById(R.id.profile_follower_count);
        followingCount = requireView().findViewById(R.id.profile_following_count);
        pinsDroppedCount = requireView().findViewById(R.id.profile_dropped_count);
        pinsFoundCount = requireView().findViewById(R.id.profile_found_count);
        location = requireView().findViewById(R.id.profile_location);
        bio = requireView().findViewById(R.id.profile_bio);
        profilePic = requireView().findViewById(R.id.profile_pic);
        Button button = requireView().findViewById(R.id.profile_button);

        Bundle args = getArguments();
        String uid =
                args != null ? ProfilePageFragmentArgs.fromBundle(getArguments()).getUid() : null;
        if (uid == null) {
            // UID is null so this is current user profile
            uid = firebase.getCurrentUser().getUid();
            // Viewing user's own profile, button is for editing
            button.setText(R.string.edit_profile_text);
            button.setOnClickListener((buttonView) -> navController.navigate(
                    ProfilePageFragmentDirections.editProfile()));
        } else {
            // Viewing someone else's profile, button is for following
            button.setOnClickListener((buttonView) -> {
                // TODO: implement user following
            });
        }
        User cachedUser = firebase.getCachedUser(uid);
        if (cachedUser != null) {
            setUserData(cachedUser);
        }
        firebase.fetchUser(uid).addOnCompleteListener(task -> setUserData(task.getResult()));

        tabLayout = requireView().findViewById(R.id.tab_layout);
        viewPager = requireView().findViewById(R.id.view_pager);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.activity_text));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.dropped_pins_text));
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

    public void setUserData(@NonNull User user) {
        username.setText(user.getUsername());
        followerCount.setText(String.valueOf(user.getNumFollowers()));
        followingCount.setText(String.valueOf(user.getNumFollowing()));
        pinsDroppedCount.setText(String.valueOf(user.getNumPinsDropped()));
        pinsFoundCount.setText(String.valueOf(user.getNumPinsFound()));
        location.setText(user.getLocation());
        bio.setText(user.getBio());
        if (user.getProfilePicUrl() != null)
            Glide.with(this).load(user.getProfilePicUrl()).into(profilePic);
    }
}