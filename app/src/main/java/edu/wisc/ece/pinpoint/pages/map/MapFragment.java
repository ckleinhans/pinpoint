package edu.wisc.ece.pinpoint.pages.map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.LocationDriver;

public class MapFragment extends Fragment {
    private Location lastKnownLocation;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment\
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        SupportMapFragment supportMapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(googleMap -> {
            styleMap(googleMap);
            getDeviceLocation(googleMap);
            googleMap.setOnInfoWindowClickListener(marker -> {
                Toast.makeText(requireContext(), "Window clicked for marker "+marker.getTag().toString(), Toast.LENGTH_SHORT).show();
            });
        });
        return view;
    }

    private void styleMap(GoogleMap map){
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

    private void loadUndiscoveredPins(Object key, Object val, GoogleMap map){
        // TODO: check if nearby pin ID is in user collection. if it is, don't add from here. Add from followup fetch of found pins
        String valString = val.toString();
        LatLng pinLocation = new LatLng(Double.parseDouble(
                valString.substring(valString.lastIndexOf("=")+1,valString.indexOf("}"))),
                Double.parseDouble(val.toString().substring(valString.indexOf("=")+1,val.toString()
                        .indexOf(","))));
        // TODO: maybe change color based on type of undiscovered pin (stranger->red, friend->green, NFC->cyan)
        Marker pinMarker = map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                .alpha(0.5f)
                .position(pinLocation));
        pinMarker.setTag(key.toString());
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation(GoogleMap map) {
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        if(getActivity() != null) {
            if(LocationDriver.getInstance(getActivity()).hasLocationOn(requireContext())) {
                // Get location and nearby undiscovered pins
                Task<Location> locationResult = LocationDriver.getInstance(requireActivity())
                        .getLastLocation(requireContext()).addOnSuccessListener(location ->
                                FirebaseDriver.getInstance().fetchNearbyPins(location)
                                        .addOnSuccessListener(pins -> pins.forEach((key, val) ->
                                                loadUndiscoveredPins(key, val, map))));
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