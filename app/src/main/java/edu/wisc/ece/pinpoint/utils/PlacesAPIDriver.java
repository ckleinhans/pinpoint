package edu.wisc.ece.pinpoint.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.Place.Type;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class PlacesAPIDriver {
    private static final HashSet<Type> NEARBY_PLACE_TYPE_FILTER = new HashSet<>(
            Arrays.asList(Type.AIRPORT, Type.AMUSEMENT_PARK, Type.AQUARIUM, Type.ART_GALLERY,
                    Type.BAKERY, Type.BAR, Type.BOOK_STORE, Type.BOWLING_ALLEY, Type.CAFE,
                    Type.CAMPGROUND, Type.CASINO, Type.CHURCH, Type.GYM, Type.HINDU_TEMPLE,
                    Type.LIBRARY, Type.MOSQUE, Type.MUSEUM, Type.NATURAL_FEATURE, Type.NIGHT_CLUB,
                    Type.PARK, Type.RESTAURANT, Type.PRIMARY_SCHOOL, Type.SECONDARY_SCHOOL,
                    Type.SCHOOL, Type.SHOPPING_MALL, Type.STADIUM, Type.SYNAGOGUE,
                    Type.TOURIST_ATTRACTION, Type.UNIVERSITY, Type.ZOO));
    private static PlacesAPIDriver instance;
    private final PlacesClient client;

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
        client = Places.createClient(context);
    }

    public static PlacesAPIDriver getInstance(Context context) {
        if (instance == null) {
            new PlacesAPIDriver(context);
        }
        return instance;
    }

    @RequiresPermission(anyOf = {"android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION"})
    public Task<List<String>> getNearbyPlaces() {
        List<Place.Field> placeFields =
                Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.TYPES);
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);
        return client.findCurrentPlace(request).continueWith(task -> {
            List<String> places = new ArrayList<>();
            List<PlaceLikelihood> likelihoods = task.getResult().getPlaceLikelihoods();
            // If no places found, return empty list
            if (likelihoods.size() == 0) {
                return places;
            }
            // If list non-empty, get city & state combo from first item to add as first element
            // Note: only works for US addresses, ok since most users will be in US
            //noinspection ConstantConditions
            String[] addrParts = likelihoods.get(0).getPlace().getAddress().split(", ");
            String city = addrParts[addrParts.length - 3].trim();
            String state = addrParts[addrParts.length - 2].trim().substring(0, 2);
            places.add(String.format("%s, %s", city, state));
            // Iterate through remaining places to filter out irrelevant types
            for (PlaceLikelihood likelihood : likelihoods) {
                if (likelihood.getPlace().getTypes() != null)
                    for (Type type : likelihood.getPlace().getTypes()) {
                        if (NEARBY_PLACE_TYPE_FILTER.contains(type)) {
                            places.add(likelihood.getPlace().getName());
                            break;
                        }
                    }
            }
            return places;
        });
    }
}
