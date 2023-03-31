package edu.wisc.ece.pinpoint;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.LocationDriver;
import edu.wisc.ece.pinpoint.utils.NotificationDriver;

public class MainActivity extends AppCompatActivity {
    private static final List<Integer> hiddenNavbarFragments =
            Arrays.asList(R.id.settings_container_fragment, R.id.edit_profile_fragment,
                    R.id.navbar_newpin);
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
    }

    public void onMapButtonClick(View view) {
        navController.navigate(R.id.navbar_map);
        // Uncomment to test cloud function
        LocationDriver.getInstance(this).getCurrentLocation(this).addOnSuccessListener(
                location -> FirebaseDriver.getInstance().fetchNearbyPins(location)
                        .addOnSuccessListener(pins -> pins.forEach(
                                (key, val) -> Log.d("PIN ID: " + key, String.valueOf(val)))));
    }
}