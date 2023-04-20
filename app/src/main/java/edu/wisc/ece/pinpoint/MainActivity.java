package edu.wisc.ece.pinpoint;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.NotificationDriver;
import edu.wisc.ece.pinpoint.utils.PinNotificationActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final List<Integer> hiddenNavbarFragments =
            Arrays.asList(R.id.settings_container_fragment, R.id.edit_profile_fragment,
                    R.id.new_pin_fragment, R.id.pin_view);
    private NavController navController;
    private ViewSwitcher switcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navController = Navigation.findNavController(this, R.id.activity_main_nav_host_fragment);
        switcher = findViewById(R.id.view_switcher);
        showView(R.id.loading_view);
        BottomNavigationView navBar = findViewById(R.id.navBar);
        BottomAppBar navBarContainer = findViewById(R.id.bottomBar);
        FloatingActionButton mapButton = findViewById(R.id.mapButton);
        NotificationDriver.getInstance(this);
        navBar.getMenu().getItem(2).setEnabled(false);
        NavigationUI.setupWithNavController(navBar, navController);
        // Set nav bar visibility
        navController.addOnDestinationChangedListener((navController, navDestination, bundle) -> {
            if (hiddenNavbarFragments.contains(navDestination.getId())) {
                navBarContainer.setVisibility(View.GONE);
                mapButton.setVisibility(View.GONE);
            } else {
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
        Tasks.whenAllComplete(fetchTasks).addOnCompleteListener(fetchingComplete -> showView(R.id.content_view));


        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            PeriodicWorkRequest saveRequest =
                    new PeriodicWorkRequest.Builder(PinNotificationActivity.class, 16,
                            TimeUnit.MINUTES)
                            // Constraints
                            .build();

            WorkManager work = WorkManager.getInstance(getApplicationContext());
            work.enqueue(saveRequest);
        }, 1);
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