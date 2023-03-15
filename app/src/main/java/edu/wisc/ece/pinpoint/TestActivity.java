package edu.wisc.ece.pinpoint;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

public class TestActivity extends AppCompatActivity {

    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        tabLayout = findViewById(R.id.tabbb_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Activity"));
        tabLayout.addTab(tabLayout.newTab().setText("Dropped Pins"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
    }
}