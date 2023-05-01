package edu.wisc.ece.pinpoint.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import java.util.HashMap;

import edu.wisc.ece.pinpoint.data.NearbyPinData;


public class LocationChangeDetection extends Worker {

    FirebaseDriver firebaseDriver;
    private static final double ONE_MILE_LATITUDE_DEGREES = 0.014492753623188;
    String x;
    Context context;
    HashMap<String, NearbyPinData> nearbyPins;

    private Location newLoc;

    SharedPreferences preferences;
    NotificationDriver notificationDriver;
    private static final double EARTH_RADIUS_MILES = 3959;



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

                    preferences = PreferenceManager.getDefaultSharedPreferences(context);

                    if(preferences.contains("counter2")) {
                    }
                    else{

                    }

                    newLoc = task.getResult();

                    if (newLoc != null) {
                        preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        if (preferences.contains("longitude") && preferences.contains("latitude")) {

                            double newLat = newLoc.getLatitude();
                            double newLong = newLoc.getLongitude();
                            double distance = calcDistanceMiles(newLat, Double.parseDouble(preferences.getString("latitude", "")),
                                    newLong, Double.parseDouble(preferences.getString("longitude", "")));
                            Log.d("dist", String.valueOf(distance));

                            if (newLoc != null && (distance <= 0.5)) {
                                

                            } else {
                                firebaseDriver = FirebaseDriver.getInstance();
                                firebaseDriver.fetchNearbyPins(newLoc).addOnCompleteListener(task1 -> {
                                    nearbyPins = task1.getResult();
                                    int i = nearbyPins.size();
                                    x = String.valueOf(i);
                                    notificationDriver = NotificationDriver.getInstance(context);
                                    notificationDriver.updatePersistent("Pins", x);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("longitude", String.valueOf(newLoc.getLongitude()));
                                    editor.apply();
                                    editor.putString("latitude", String.valueOf(newLoc.getLatitude()));
                                    editor.apply();

                                });
                            }
                        } else {

                            preferences.edit().remove("longitude").commit();
                            preferences.edit().remove("latitude").commit();
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("longitude", String.valueOf(newLoc.getLongitude()));
                            editor.apply();
                            editor.putString("latitude", String.valueOf(newLoc.getLatitude()));
                            editor.apply();
                        }

                    }
                });
            }



        }, 1);

        return Result.retry();
    }

    public static double calcDistanceMiles(double lat1, double lat2, double long1, double long2) {
        double oneMileLongitudeDegrees = calcOneMileLongitudeDegrees(lat1);
        double latitudeDiffMiles = (lat2 - lat1) / ONE_MILE_LATITUDE_DEGREES;
        double longitudeDiffMiles = (long2 - long1) / oneMileLongitudeDegrees;
        return Math.sqrt(Math.pow(latitudeDiffMiles, 2) + Math.pow(longitudeDiffMiles, 2));
    }

    private static double calcOneMileLongitudeDegrees(double latitude) {
        return 1 / ((Math.PI / 180) * EARTH_RADIUS_MILES * Math.cos(latitude));
    }
}
