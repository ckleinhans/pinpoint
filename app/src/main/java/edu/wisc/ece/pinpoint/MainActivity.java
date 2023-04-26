package edu.wisc.ece.pinpoint;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ViewSwitcher;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.NotificationDriver;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final List<Integer> hiddenNavbarFragments =
            Arrays.asList(R.id.settings_container_fragment, R.id.edit_profile_fragment,
                    R.id.new_pin_fragment);
    private int currentDestinationId;
    private NavController navController;
    private NavHostFragment navHostFragment;
    private ViewSwitcher switcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate view switcher & start on loading screen
        switcher = findViewById(R.id.view_switcher);
        showView(R.id.loading_view);
        // Instantiate nav components
        BottomNavigationView navBar = findViewById(R.id.navBar);
        BottomAppBar navBarContainer = findViewById(R.id.bottomBar);
        FloatingActionButton mapButton = findViewById(R.id.mapButton);
        NotificationDriver.getInstance(this);
        // Disable hidden middle button
        navBar.getMenu().getItem(2).setEnabled(false);

        // hide bottom app bar when keyboard opened
        KeyboardVisibilityEvent.setEventListener(this, (isOpen) -> {
            if (isOpen) {
                navBarContainer.setVisibility(View.GONE);
                mapButton.setVisibility(View.GONE);
            } else if (!hiddenNavbarFragments.contains(currentDestinationId)) {
                navBarContainer.setVisibility(View.VISIBLE);
                mapButton.setVisibility(View.VISIBLE);
            }
        });

        // Fetch logged in user profile, following/followers, & activity on app load
        FirebaseDriver firebase = FirebaseDriver.getInstance();
        String uid = firebase.getUid();
        // List of fetching tasks that must be completed before launching app content
        List<Task<Object>> fetchTasks = new ArrayList<>();
        // Tasks must be continued with Object tasks to be added to fetch list
        fetchTasks.add(firebase.fetchUser(uid).continueWith(t -> null));
        fetchTasks.add(firebase.fetchFollowing(uid).continueWith(t -> null));
        fetchTasks.add(firebase.fetchFollowers(uid).continueWith(t -> null));
        fetchTasks.add(firebase.fetchActivity(uid).continueWith(t -> null));
        fetchTasks.add(firebase.fetchDroppedPins()
                .addOnSuccessListener(pids -> Log.d(TAG, "Successfully fetched dropped pins."))
                .addOnFailureListener(e -> Log.w(TAG, e)).continueWith(t -> null));
        fetchTasks.add(firebase.fetchFoundPins()
                .addOnSuccessListener(pids -> Log.d(TAG, "Successfully fetched found pins."))
                .addOnFailureListener(e -> Log.w(TAG, e)).continueWith(t -> null));
        // Wait until all tasks complete before showing view
        Tasks.whenAllComplete(fetchTasks).addOnCompleteListener(fetchingComplete -> {
            // Instantiate nav host and inject into view
            navHostFragment = NavHostFragment.create(R.navigation.navigation);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_placeholder, navHostFragment).commitNow();
            // Set up nav controller
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(navBar, navController);
            // Set nav bar visibility
            navController.addOnDestinationChangedListener(
                    (navController, navDestination, bundle) -> {
                        currentDestinationId = navDestination.getId();
                        if (hiddenNavbarFragments.contains(navDestination.getId())) {
                            navBarContainer.setVisibility(View.GONE);
                            mapButton.setVisibility(View.GONE);
                        } else {
                            navBarContainer.setVisibility(View.VISIBLE);
                            mapButton.setVisibility(View.VISIBLE);
                        }
                    });
            // call default on item selcted listener but override result to always return true to
            // highlight the selected navbar item
            navBar.setOnItemSelectedListener(item -> {
                NavigationUI.onNavDestinationSelected(item, navController);
                return true;
            });
            // add on item reselected listener to navigate to top level fragment
            navBar.setOnItemReselectedListener(item -> navController.navigate(item.getItemId()));
            // add on back button handler to handle in app back navigation
            OnBackPressedCallback callback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (navController.getPreviousBackStackEntry() != null)
                        navController.popBackStack();
                    else finish();
                }
            };
            getOnBackPressedDispatcher().addCallback(this, callback);
            // Switch to app view once loading is complete
            showView(R.id.content_view);
        });

    }

    public void onMapButtonClick(View view) {
        navController.navigate(R.id.navbar_map);
    }

    private void showView(int viewId) {
        if (switcher.getNextView().getId() == viewId) {
            switcher.showNext();
        }
    }
}