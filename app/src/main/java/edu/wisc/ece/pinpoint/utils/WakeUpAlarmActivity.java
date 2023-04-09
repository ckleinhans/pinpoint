package edu.wisc.ece.pinpoint.utils;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.work.ListenableWorker;

import com.google.android.gms.tasks.Task;

import java.util.Map;

public class WakeUpAlarmActivity {

    private Task<Location> location;
    private Location loc;
    private Task<Location> lastLocation;
    double lat = 5.0;
    Object l;

    FirebaseDriver firebaseDriver;
    Map<String, Object> nearbyPins;

    Context context;
    String x;

    public WakeUpAlarmActivity() {

        LocationDriver locationDriver = LocationDriver.getInstance(context);

        location = locationDriver.getCurrentLocation(context);
        lastLocation = locationDriver.getLastLocation(context);

        locationDriver.getCurrentLocation(context).addOnCompleteListener(task -> {
            loc = task.getResult();
            if (loc != null) {
                firebaseDriver = FirebaseDriver.getInstance();
                firebaseDriver.fetchNearbyPins(loc).addOnCompleteListener(task1 -> {
                    nearbyPins = task1.getResult();
                    int i = nearbyPins.size();
                    x = String.valueOf(i);
                    NotificationDriver notificationDriver = NotificationDriver.getInstance(null);
                    notificationDriver.sendOneShot("Pins", x + " pins found nearby");
                });
            } else {
                NotificationDriver notificationDriver = NotificationDriver.getInstance(null);
                notificationDriver.sendOneShot("Pins", "no pins");

            }
        });
    }
}


