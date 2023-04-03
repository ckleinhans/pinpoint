package edu.wisc.ece.pinpoint.pages.map;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.pages.newpin.NewPinFragmentDirections;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.LocationDriver;

public class MapFragment extends Fragment {
    private Location lastKnownLocation;
    private GoogleMap map;
    private NavController navController;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            map.setOnInfoWindowClickListener(marker -> {
                if(marker.getAlpha() == 1f){
                    navController.navigate(edu.wisc.ece.pinpoint.pages.newpin.NewPinFragmentDirections.pinView(marker.getTag().toString()));
                    Toast.makeText(requireContext(), "Window clicked for marker "+marker.getTag(), Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(requireContext(), "Travel to this pin to reveal its contents!", Toast.LENGTH_SHORT).show();
                }
            });
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

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

    private void loadUndiscoveredPins(Object key, Object val){
        // TODO: check if nearby pin ID is in user collection. if it is, don't add from here. Add from followup fetch of found pins
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

    private void loadDiscoveredPins(String id, GeoPoint geoPoint) {
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
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        // Get dropped pins
        FirebaseDriver.getInstance().getDroppedPins().addOnSuccessListener(pins ->
                pins.forEach((id, pin) -> {
                    loadDiscoveredPins(id, pin.getLocation());
        }));
        // Get found pins
        FirebaseDriver.getInstance().getFoundPins().addOnSuccessListener(pins ->
                pins.forEach((id, pin) -> loadDiscoveredPins(id, pin.getLocation())));
        if (getActivity() != null) {
            if(LocationDriver.getInstance(getActivity()).hasLocationOn(requireContext())) {
                // Get location and nearby undiscovered pins
                Task<Location> locationResult = LocationDriver.getInstance(requireActivity())
                        .getLastLocation(requireContext()).addOnSuccessListener(location ->
                                FirebaseDriver.getInstance().fetchNearbyPins(location)
                                        .addOnSuccessListener(pins -> pins.forEach((key, val) ->
                                                loadUndiscoveredPins(key, val))));
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
            }
        }
    }
}