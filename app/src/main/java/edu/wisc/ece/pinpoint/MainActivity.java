package edu.wisc.ece.pinpoint;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import static android.content.ContentValues.TAG;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Log;
import android.provider.Settings;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.wisc.ece.pinpoint.utils.ActivityRecognitionIntentService;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.LocationChangeDetection;
import edu.wisc.ece.pinpoint.utils.NotificationDriver;

public class MainActivity extends AppCompatActivity {
    private static final List<Integer> hiddenNavbarFragments =
            Arrays.asList(R.id.settings_container_fragment, R.id.edit_profile_fragment,
                    R.id.new_pin_fragment);
    private FirebaseDriver firebase;
    private int currentDestinationId;
    private NavController navController;
    private NavHostFragment navHostFragment;
    private ViewSwitcher switcher;
    private PendingIntent mPendingIntent;
    private final String TRANSITION_ACTION_RECEIVER =
            BuildConfig.APPLICATION_ID + "TRANSITION_ACTION_RECEIVER";
    private ActivityRecognitionIntentService activityRecognitionIntentService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpTransitions();
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION},
                1);

        firebase = FirebaseDriver.getInstance();

        // Create trace to track initial app load time
        Trace trace = FirebasePerformance.getInstance().newTrace("initialLoad");
        trace.start();

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
        String uid = firebase.getUid();
        // List of fetching tasks that must be completed before launching app content
        List<Task<Object>> fetchTasks = new ArrayList<>();
        // Tasks must be continued with Object tasks to be added to fetch list
        fetchTasks.add(firebase.fetchUser(uid).continueWith(t -> null));
        fetchTasks.add(firebase.fetchFollowing(uid).continueWith(t -> null));
        fetchTasks.add(firebase.fetchFollowers(uid).continueWith(t -> null));
        fetchTasks.add(firebase.fetchActivity(uid).continueWith(t -> null));
        fetchTasks.add(firebase.fetchDroppedPins().continueWith(t -> null));
        fetchTasks.add(firebase.fetchFoundPins().continueWith(t -> null));
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
            // End tracking of initial load since app is now loaded
            trace.stop();
        });

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {

            // TODO: do we need to delete these here? It'd be better to just do a null check in
            //  the other activity
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().remove("longitude").apply();
            preferences.edit().remove("latitude").apply();

            if(preferences.contains("counter2")){

            }else{
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("counter2", String.valueOf(1));
                editor.apply();

                new AlertDialog.Builder(this)
                        .setTitle("Enable Background Location")
                        .setMessage("Enabling background location access allows PinPoint to search for nearby pins when app is closed ")

                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                                intent.setData(uri);
                                getApplicationContext().startActivity(intent);
                                Toast.makeText(getApplicationContext(),"Permissions -> Location -> Allow all the time",Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }

            PeriodicWorkRequest saveRequest =
                    new PeriodicWorkRequest.Builder(LocationChangeDetection.class, 1,
                            TimeUnit.MINUTES)
                            // Constraints
                            .build();

            WorkManager work = WorkManager.getInstance(getApplicationContext());
            work.enqueue(saveRequest);
        }, 1000);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check for new app versions for testers using Firebase App Distribution
        if (firebase.isTesterSignedIn()) firebase.checkForNewTesterRelease();
    }

    public void onMapButtonClick(View view) {
        navController.navigate(R.id.navbar_map);
    }

    private void showView(int viewId) {
        if (switcher.getNextView().getId() == viewId) {
            switcher.showNext();
        }
    }


    private void setUpTransitions() {
        List<ActivityTransition> transitions = new ArrayList<>();

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        // Register for Transitions Updates.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

//        Intent intent = new Intent(TRANSITION_ACTION_RECEIVER);
//        mPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_MUTABLE);
//        activityRecognitionIntentService = new ActivityRecognitionIntentService();
//        registerReceiver(activityRecognitionIntentService, new IntentFilter(TRANSITION_ACTION_RECEIVER));


        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);
        intent.setAction(ActivityRecognitionIntentService.INTENT_ACTION);


        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_MUTABLE);

        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, pendingIntent);
        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "Transitions Api was successfully registered.");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Transitions Api could not be registered: " + e);
                    }
                });
    }
}