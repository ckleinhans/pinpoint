package edu.wisc.ece.pinpoint;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class MainActivity extends AppCompatActivity {
    private FirebaseDriver firebase;
    private BottomNavigationView navBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebase = FirebaseDriver.getInstance();
        navBar = findViewById(R.id.navBar);
        navBar.getMenu().getItem(2).setEnabled(false);
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