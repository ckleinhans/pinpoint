package edu.wisc.ece.pinpoint.pages.map;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.wisc.ece.pinpoint.MainActivity;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.OrderedHashSet;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;
import edu.wisc.ece.pinpoint.utils.LocationDriver;

public class MapFragment extends Fragment {
    private Location lastKnownLocation;
    private GoogleMap map;
    private NavController navController;
    private final FirebaseDriver firebase = FirebaseDriver.getInstance();
    private static final String TAG = MainActivity.class.getName();
    private final List<Task<OrderedHashSet<String>>> pinTasks = new ArrayList<>();
    private boolean pinsLoaded = false;
    private Long pinnieCount;
    private ProgressBar pinnieProgressBar;
    private Drawable pinnies_logo;
    private TextView pinniesText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(firebase.getCachedFoundPinIds() == null && firebase.getCachedFoundPinIds() == null){
            pinsLoaded = true;
            pinTasks.add(firebase.fetchDroppedPins()
                    .addOnSuccessListener(pids ->
                            Log.d(TAG, "Successfully fetched dropped pins."))
                    .addOnFailureListener(e -> Log.w(TAG, e)));
            pinTasks.add(firebase.fetchFoundPins()
                    .addOnSuccessListener(pids ->
                            Log.d(TAG, "Successfully fetched found pins."))
                    .addOnFailureListener(e -> Log.w(TAG, e)));
            Tasks.whenAllComplete(pinTasks).addOnCompleteListener(pinFetchingComplete -> loadDiscoveredPins());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        SupportMapFragment supportMapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(googleMap -> {
            this.map = googleMap;
            styleMap();
            getDeviceLocation();
            if(!pinsLoaded) {
                loadDiscoveredPins();
            }
            map.setOnInfoWindowClickListener(marker -> {
                if(marker.getAlpha() == 1f){
                    navController.navigate(edu.wisc.ece.pinpoint.pages.newpin.NewPinFragmentDirections.pinView(marker.getTag().toString()));
                }
                else {
                    Toast.makeText(requireContext(), R.string.undiscovered_pin_message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        pinnieProgressBar = requireView().findViewById(R.id.map_pinnies_progress);
        pinniesText = requireView().findViewById(R.id.map_pinnies_text);

        setPinniesDrawable();
        setPinnieCount();


    }

    private void styleMap(){
        if(getActivity() != null){
            map.setInfoWindowAdapter(new InfoAdapter(requireContext()));
            int nightModeFlags =
                    requireActivity().getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
            switch (nightModeFlags) {
                case Configuration.UI_MODE_NIGHT_YES:
                    map.setMapStyle(MapStyleOptions
                            .loadRawResourceStyle(requireContext(), R.raw.map_style_dark));
                    break;

                case Configuration.UI_MODE_NIGHT_NO:
                    map.setMapStyle(MapStyleOptions
                            .loadRawResourceStyle(requireContext(), R.raw.map_style_light));
                    break;

                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    break;
            }
        }
    }

    private void loadDiscoveredPins(){
        // Get dropped pins
        Iterator<String> droppedIterator = firebase.getCachedDroppedPinIds().getIterator();
        while (droppedIterator.hasNext()){
            String pinId = droppedIterator.next();
            createDiscoveredPin(pinId, firebase.getCachedPin(pinId).getLocation());
        }
        // Get found pins
        Iterator<String> foundIterator = firebase.getCachedFoundPinIds().getIterator();
        while (foundIterator.hasNext()){
            String pinId = foundIterator.next();
            createDiscoveredPin(pinId, firebase.getCachedPin(pinId).getLocation());
        }
    }

    private void createUndiscoveredPin(Object key, Object val){
        String valString = val.toString();
        LatLng pinLocation = new LatLng(Double.parseDouble(
                valString.substring(valString.lastIndexOf("=")+1,valString.indexOf("}"))),
                Double.parseDouble(valString.substring(valString.indexOf("=")+1,valString
                        .indexOf(","))));
        // TODO: change color based on source of pin (general->red, friend->green, NFC->cyan)
        Marker pinMarker = map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .alpha(.5f)
                .position(pinLocation));
        pinMarker.setTag(key);
    }

    private void createDiscoveredPin(String id, GeoPoint geoPoint) {
        LatLng pinLocation = new LatLng(geoPoint.getLatitude(),geoPoint.getLongitude());
        // TODO: change color based on source of pin (general->red, friend->green, NFC->cyan)
        Marker pinMarker = map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .alpha(1f)
                .position(pinLocation));
        pinMarker.setTag(id);
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        LocationDriver locationDriver = LocationDriver.getInstance(requireContext());
        map.setPadding(0,0,0,150);
        if (getActivity() != null) {
            Tasks.whenAllComplete(pinTasks).addOnCompleteListener(pinFetchingComplete -> {
            if(locationDriver.hasCoarseLocation(requireContext()) & locationDriver.hasLocationOn(requireContext())) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
                // Get location and nearby undiscovered pins
                Task<Location> locationResult = locationDriver.getLastLocation(requireContext())
                        .addOnSuccessListener(location ->
                                FirebaseDriver.getInstance().fetchNearbyPins(location)
                                        .addOnSuccessListener(pins -> pins.forEach((key, val) -> {
                                            // load pin if undiscovered
                                            if(!firebase.getCachedDroppedPinIds().contains(key) &
                                                    !firebase.getCachedFoundPinIds().contains(key))
                                                createUndiscoveredPin(key, val);
                                        })));
                locationResult.addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lastKnownLocation.getLatitude(),
                                            lastKnownLocation.getLongitude()), 16));
                        }
                    }
                });
            }});
        }
    }

    private void setPinniesDrawable(){
        TypedValue colorValue = new TypedValue();
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSecondary, colorValue, true);
        Drawable unwrapped_pinnies_logo = AppCompatResources.getDrawable(getContext(), R.drawable.ic_pinnies_logo);
        pinnies_logo = DrawableCompat.wrap(unwrapped_pinnies_logo);
        DrawableCompat.setTint(pinnies_logo, colorValue.data);
    }

    private void setPinnieCount() {
        FirebaseDriver driver = FirebaseDriver.getInstance();
        String uid = driver.getCurrentUser().getUid();

        if(driver.getCachedPinnies(uid) != null){
            pinnieCount = driver.getCachedPinnies(uid);
            setPinniesUI();
            return;
        }

        driver.getPinnies(uid).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                pinnieCount = task.getResult();
                Log.d(TAG, String.format("Got currency for user %s: %s", uid, pinnieCount.toString()));
                setPinniesUI();
            } else {
                Log.d(TAG, "get failed with ", task.getException());
            }
        });
    }

    private void setPinniesUI() {
        pinniesText.setText(FormatUtils.humanReadablePinnies(pinnieCount));
        pinniesText.setCompoundDrawablesWithIntrinsicBounds(null, null, pinnies_logo, null);
        pinnieProgressBar.setVisibility(View.GONE);
        pinniesText.setVisibility(View.VISIBLE);
    }
}