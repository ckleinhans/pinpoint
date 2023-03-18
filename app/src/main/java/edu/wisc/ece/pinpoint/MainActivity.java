package edu.wisc.ece.pinpoint;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class MainActivity extends AppCompatActivity {
    private FirebaseDriver firebase;
    private FloatingActionButton mapButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebase = FirebaseDriver.getInstance();
        NavController navController = Navigation.findNavController(this, R.id.activity_main_nav_host_fragment);
        BottomNavigationView navBar = findViewById(R.id.navBar);
        navBar.getMenu().getItem(2).setEnabled(false);
        NavigationUI.setupWithNavController(navBar, navController);
        mapButton = findViewById(R.id.mapButton);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.navbarMap);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is not signed in, if so start auth flow
        if (firebase.getUser() == null) {
            firebase.launchAuth(this);
        } else {
            // User is logged in!
            // Log user out for testing purposes
            firebase.logout(this);
        }
    }
}