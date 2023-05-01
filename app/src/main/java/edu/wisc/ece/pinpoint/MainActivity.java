package edu.wisc.ece.pinpoint;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ViewSwitcher;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.LocationChangeDetection;
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

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().remove("longitude").commit();
            preferences.edit().remove("latitude").commit();

            if(preferences.contains("counter2")){

            }else{

                new AlertDialog.Builder(this)
                        .setTitle("Enable Background Location")
                        .setMessage("To get notifications all the time")

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                                intent.setData(uri);
                                getApplicationContext().startActivity(intent);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("counter2", String.valueOf(1));
                                editor.apply();
                            }
                        })

                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();

            }

            PeriodicWorkRequest saveRequest =
                    new PeriodicWorkRequest.Builder(LocationChangeDetection.class, 30,
                            TimeUnit.MINUTES)
                            // Constraints
                            .build();

            WorkManager work = WorkManager.getInstance(getApplicationContext());
            work.enqueue(saveRequest);
        }, 1000);
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