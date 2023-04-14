package edu.wisc.ece.pinpoint;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.NotificationDriver;
import edu.wisc.ece.pinpoint.utils.PinNotificationActivity;

public class MainActivity extends AppCompatActivity {
    private static final List<Integer> hiddenNavbarFragments =
            Arrays.asList(R.id.settings_container_fragment, R.id.edit_profile_fragment,
                    R.id.new_pin_fragment);
    private NavController navController;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navController = Navigation.findNavController(this, R.id.activity_main_nav_host_fragment);
        BottomNavigationView navBar = findViewById(R.id.navBar);
        BottomAppBar navBarContainer = findViewById(R.id.bottomBar);
        FloatingActionButton mapButton = findViewById(R.id.mapButton);
        NotificationDriver.getInstance(this);
        navBar.getMenu().getItem(2).setEnabled(false);
        NavigationUI.setupWithNavController(navBar, navController);

        navController.addOnDestinationChangedListener((navController, navDestination, bundle) -> {
            if (hiddenNavbarFragments.contains(navDestination.getId())) {
                navBarContainer.setVisibility(View.GONE);
                mapButton.setVisibility(View.GONE);
            } else {
                navBarContainer.setVisibility(View.VISIBLE);
                mapButton.setVisibility(View.VISIBLE);
            }
        });


        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    PeriodicWorkRequest saveRequest =
                                            new PeriodicWorkRequest.Builder(PinNotificationActivity.class, 15, TimeUnit.MINUTES)
                                                    // Constraints
                                                    .build();
        // Fetch logged in user profile, followers/following, & activity on app load
        FirebaseDriver firebase = FirebaseDriver.getInstance();
        String uid = firebase.getCurrentUser().getUid();
        firebase.fetchUser(uid);
        firebase.fetchSocials(uid);
        firebase.fetchActivity(uid);

        PeriodicWorkRequest saveRequest =
                new PeriodicWorkRequest.Builder(PinNotificationActivity.class, 20, TimeUnit.MINUTES)
                        // Constraints
                        .build();

                                    WorkManager work = WorkManager.getInstance(getApplicationContext());
                                    work.enqueue(saveRequest);
                                }


        },1);

    }




    public void onMapButtonClick(View view) {
        navController.navigate(R.id.navbar_map);
    }


}