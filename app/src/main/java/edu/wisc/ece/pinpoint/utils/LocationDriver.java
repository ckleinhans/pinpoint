package edu.wisc.ece.pinpoint.utils;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

public class LocationDriver {
    private static LocationDriver instance;
    private final FusedLocationProviderClient fusedLocationClient;

    private LocationDriver(Context context) {
        if (instance != null) {
            throw new IllegalStateException("LocationDriver has already been instantiated.");
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        instance = this;
    }

    public static LocationDriver getInstance(Context context) {
        if (instance == null) {
            new LocationDriver(context);
        }
        return instance;
    }

    @SuppressLint("MissingPermission")
    public Task<Location> getLastLocation(Context context) {
        if (!hasFineLocation(context) && !hasCoarseLocation(context)) {
            throw new IllegalStateException("User has not provided location permissions!");
        } else {
            return fusedLocationClient.getLastLocation();
        }
    }

    @SuppressLint("MissingPermission")
    public Task<Location> getCurrentLocation(Context context) {
        if (!hasFineLocation(context) && !hasCoarseLocation(context)) {
            throw new IllegalStateException("User has not provided location permissions!");
        } else {
            return fusedLocationClient.getCurrentLocation(PRIORITY_BALANCED_POWER_ACCURACY, null);
        }
    }

    public boolean hasCoarseLocation(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
    }

    public boolean hasFineLocation(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                ACCESS_FINE_LOCATION) == PERMISSION_GRANTED;
    }

    public boolean hasLocationOn(Context context) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
