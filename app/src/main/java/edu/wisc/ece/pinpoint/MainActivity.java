package edu.wisc.ece.pinpoint;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

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
        navBar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()){
                    case R.id.navbarProfile:
                        startActivity(new Intent(getApplicationContext(), ProfilePageActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.navbarSearch:
                        return true;
                    case R.id.navbarLeaderboard:
                        return true;
                    case R.id.navbarFeed:
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        navBar.setSelectedItemId(R.id.navbarEmpty);
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