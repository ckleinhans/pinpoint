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
    private NavController navController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebase = FirebaseDriver.getInstance();
        navController = Navigation.findNavController(this, R.id.activity_main_nav_host_fragment);
        BottomNavigationView navBar = findViewById(R.id.navBar);
        navBar.getMenu().getItem(2).setEnabled(false);
        NavigationUI.setupWithNavController(navBar, navController);
    }

    public void onMapButtonClick(View view) {
        navController.navigate(R.id.navbarMap);
        // TODO: temporary use map button to log user out, remove later
        firebase.logout(MainActivity.this);
    }
}