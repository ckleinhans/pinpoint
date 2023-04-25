package edu.wisc.ece.pinpoint.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;

public class LocationChangeDetection extends Worker {

    FirebaseDriver firebaseDriver;
    String x;
    Context context;
    Map<String, Map<String, Object>> nearbyPins;
    private Location newLoc;

    SharedPreferences preferences;
    NotificationDriver notificationDriver;


    public LocationChangeDetection(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {

            LocationDriver locationDriver = LocationDriver.getInstance(context);

            if (locationDriver.hasCoarseLocation(context)) {
                locationDriver.getLastLocation(context).addOnCompleteListener(task -> {

                        newLoc = task.getResult();
                    if (newLoc != null) {
                        preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        if (preferences.contains("long")) {
                            if (newLoc != null && (newLoc.getLongitude() == (Double.parseDouble(preferences.getString("long", ""))))) {
                                Log.d("Tag", "SameLocation");
                            } else {
                                Log.d("new", String.valueOf(newLoc.getLongitude()));
                                Log.d("old", preferences.getString("long",""));
                                Log.d("Tag", "diff");
                                firebaseDriver = FirebaseDriver.getInstance();
                                firebaseDriver.fetchNearbyPins(newLoc).addOnCompleteListener(task1 -> {
                                    nearbyPins = task1.getResult();
                                    int i = nearbyPins.size();
                                    x = String.valueOf(i);
                                    notificationDriver = NotificationDriver.getInstance(context);
                                    notificationDriver.updatePersistent("Pins", x);

                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("long", String.valueOf(newLoc.getLongitude()));
                                    editor.apply();

                                });
                            }
                        } else {
                            preferences.edit().remove("long").commit();
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("long", String.valueOf(newLoc.getLongitude()));
                            editor.apply();
                            Log.d("Tag", "location not set");

                        }

                    }
                });


            }


        }, 1000);

        return Result.retry();
    }
}
