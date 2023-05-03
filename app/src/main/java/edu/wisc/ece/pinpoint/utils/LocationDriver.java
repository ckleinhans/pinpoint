package edu.wisc.ece.pinpoint.utils;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;

public class LocationDriver {
    private static final double ONE_MILE_LATITUDE_DEGREES = 0.014492753623188;
    private static final double EARTH_RADIUS_MILES = 3959;
    private static final double PIN_FIND_RADIUS_MILES = 0.0095;
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

    public static boolean isCloseEnoughToFindPin(LatLng userLoc, LatLng pinLoc) {
        return calcDistanceMiles(userLoc, pinLoc) <= PIN_FIND_RADIUS_MILES;
    }

    public static double calcDistanceMiles(LatLng loc1, LatLng loc2) {
        double oneMileLongitudeDegrees = calcOneMileLongitudeDegrees(loc1.latitude);
        double latitudeDiffMiles = (loc1.latitude - loc2.latitude) / ONE_MILE_LATITUDE_DEGREES;
        double longitudeDiffMiles = (loc1.longitude - loc2.longitude) / oneMileLongitudeDegrees;
        return Math.sqrt(Math.pow(latitudeDiffMiles, 2) + Math.pow(longitudeDiffMiles, 2));
    }

    private static double calcOneMileLongitudeDegrees(double latitude) {
        return 1 / ((Math.PI / 180) * EARTH_RADIUS_MILES * Math.cos(latitude));
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

    public boolean hasBackgroundLocation(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED;
    }

    public boolean hasLocationOn(Context context) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


}
