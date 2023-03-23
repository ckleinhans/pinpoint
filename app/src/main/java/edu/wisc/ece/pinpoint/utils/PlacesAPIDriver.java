package edu.wisc.ece.pinpoint.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.google.android.libraries.places.api.Places;

public class PlacesAPIDriver {
    private static PlacesAPIDriver instance;

    private PlacesAPIDriver(Context context) {
        if (instance != null) {
            throw new IllegalStateException("FirebaseDriver has already been instantiated.");
        }
        instance = this;
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Places.initialize(context,
                    appInfo.metaData.getString("com.google.android.geo.API_KEY"));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static PlacesAPIDriver getInstance(Context context) {
        if (instance == null) {
            new PlacesAPIDriver(context);
        }
        return instance;
    }
}
