package edu.wisc.ece.pinpoint;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class MainActivity extends AppCompatActivity {
    private FirebaseDriver firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebase = FirebaseDriver.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is not signed in, if so start auth flow
        if (firebase.getUser() == null) {
            firebase.launchAuth(this);
        } else {
            // User is logged in!
        }
    }
}