package edu.wisc.ece.pinpoint.pages.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;

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
    private ConstraintLayout locationLayout;
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
        ConstraintLayout followerLayout = requireView().findViewById(R.id.profile_follower_layout);
        followerCount = requireView().findViewById(R.id.profile_follower_count);
        ConstraintLayout followingLayout =
                requireView().findViewById(R.id.profile_following_layout);
        followingCount = requireView().findViewById(R.id.profile_following_count);
        pinsDroppedCount = requireView().findViewById(R.id.profile_dropped_count);
        pinsFoundCount = requireView().findViewById(R.id.profile_found_count);
        location = requireView().findViewById(R.id.profile_location);
        locationLayout = requireView().findViewById(R.id.profile_location_layout);
        bio = requireView().findViewById(R.id.profile_bio);
        profilePic = requireView().findViewById(R.id.profile_pic);
        button = requireView().findViewById(R.id.profile_button);

        String uid = ProfilePageFragmentArgs.fromBundle(requireArguments()).getUid();

        // Don't show settings and show back button instead
        ImageButton backButton = requireView().findViewById(R.id.profile_back_button);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> navController.popBackStack());

        if (firebase.getUid().equals(uid)) {
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

        if (firebase.isUserCached(uid)) {
            setUserData(firebase.getCachedUser(uid));
        }
        firebase.fetchUser(uid).addOnCompleteListener(task -> setUserData(task.getResult()));

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

    public void setUserData(User user) {
        if (user == null) {
            // user was deleted
            if (getContext() != null)
                Toast.makeText(getContext(), R.string.deleted_user, Toast.LENGTH_SHORT).show();
            navController.popBackStack();
            return;
        }
        user.loadProfilePic(profilePic, this);
        username.setText(user.getUsername());
        followerCount.setText(FormatUtils.trimmedNumber(user.getNumFollowers()));
        followingCount.setText(FormatUtils.trimmedNumber(user.getNumFollowing()));
        pinsDroppedCount.setText(FormatUtils.trimmedNumber(user.getNumPinsDropped()));
        pinsFoundCount.setText(FormatUtils.trimmedNumber(user.getNumPinsFound()));
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

    public void setButton(String user) {
        // Viewing someone else's profile, button is for following
        final String uid = user;
        // if the user is already following this profile, show unfollow button
        if (firebase.getCachedFollowing(firebase.getUid()).contains(uid)) {
            button.setText(R.string.unfollow_text);
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.soft_red));
            button.setOnClickListener((buttonView) -> {
                buttonView.setEnabled(false);
                firebase.unfollowUser(uid).addOnSuccessListener(t -> {
                    followerCount.setText(String.valueOf(
                            Integer.parseInt(followerCount.getText().toString()) - 1));
                    setButton(user);
                    buttonView.setEnabled(true);
                }).addOnFailureListener(
                        e -> Toast.makeText(requireContext(), R.string.unfollow_error_message,
                                Toast.LENGTH_SHORT).show());
            });
        } else {
            // if this profile follows the user but the user does not, show follow back button
            if (firebase.getCachedFollowers(firebase.getUid()).contains(uid))
                button.setText(R.string.follow_back_text);
            else button.setText(R.string.follow_text);
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue));
            button.setOnClickListener((buttonView) -> {
                buttonView.setEnabled(false);
                firebase.followUser(uid).addOnSuccessListener(t -> {
                    followerCount.setText(String.valueOf(
                            Integer.parseInt(followerCount.getText().toString()) + 1));
                    setButton(user);
                    buttonView.setEnabled(true);
                }).addOnFailureListener(
                        e -> Toast.makeText(requireContext(), R.string.follow_error_message,
                                Toast.LENGTH_SHORT).show());
            });
        }
    }
}