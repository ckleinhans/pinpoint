package edu.wisc.ece.pinpoint.pages.map;

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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import edu.wisc.ece.pinpoint.R;

public class MapFragment extends Fragment {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location lastKnownLocation;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationProviderClient = LocationServices
                .getFusedLocationProviderClient(requireActivity());
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
            loadPins(googleMap);
            googleMap.setOnInfoWindowClickListener(marker -> {
                Toast.makeText(requireContext(), "Window clicked", Toast.LENGTH_SHORT).show();
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

    private void loadPins(GoogleMap map){
        LatLng unionSouth = new LatLng(43.0719999, -89.4098351);

        MarkerOptions uSouthMap = new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                .alpha(0.5f)
                .position(unionSouth)
                .title("Union South Boyyyyyyy");
        map.addMarker(uSouthMap);
    }

    private void getDeviceLocation(GoogleMap map) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                if(getActivity() != null) {
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
        } catch (SecurityException e)  {
        }
    }
}