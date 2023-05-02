package edu.wisc.ece.pinpoint.utils;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionIntentService extends BroadcastReceiver {

    NotificationDriver notificationDriver;
    public SharedPreferences preferences;

    public static final String INTENT_ACTION = "com.example.myapplication.ACTION_PROCESS_ACTIVITY_TRANSITIONS";
    @Override
    public void onReceive(Context context, Intent intent) {

        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                // chronological sequence of events....
                String theActivity = toActivityString(event.getActivityType());

                if(theActivity.equals("WALKING")){
                    preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("Activity", theActivity);
                    editor.apply();
                    notificationDriver = NotificationDriver.getInstance(context);
                    notificationDriver.updatePersistent("Activity", "Great Job getting your butt up");

                }
                else if(theActivity.equals("DRIVING")){
                    preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("Activity", theActivity);
                    editor.apply();


                }
                else {
                    preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("Activity", theActivity);
                    editor.apply();

                }

                Log.d("TAG", theActivity);


            }
        }
    }

    private static String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.WALKING:
                return "WALKING";
            case DetectedActivity.IN_VEHICLE:
                return "DRIVING";
            default:
                return "UNKNOWN";
        }
    }
}

