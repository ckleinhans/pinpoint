package edu.wisc.ece.pinpoint.pages.map;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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
import java.util.Map;

import edu.wisc.ece.pinpoint.MainActivity;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.OrderedPinMetadata;
import edu.wisc.ece.pinpoint.data.PinMetadata;
import edu.wisc.ece.pinpoint.data.PinMetadata.PinSource;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;
import edu.wisc.ece.pinpoint.utils.LocationDriver;

public class MapFragment extends Fragment {
    private static final String TAG = MainActivity.class.getName();
    private final FirebaseDriver firebase = FirebaseDriver.getInstance();
    private final List<Task<OrderedPinMetadata>> pinTasks = new ArrayList<>();
    private GoogleMap map;
    private NavController navController;
    private boolean pinsLoaded = false;
    private Long pinnieCount;
    private ProgressBar pinnieProgressBar;
    private ImageView pinnies_logo;
    private TextView pinniesText;
    private ArrayList<Marker> droppedMarkers;
    private ArrayList<Marker> friendMarkers;
    private ArrayList<Marker> nfcMarkers;
    private ArrayList<Marker> devMarkers;
    private ArrayList<Marker> generalMarkers;
    private boolean isFilterVisible = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (firebase.getCachedDroppedPinMetadata() == null && firebase.getCachedFoundPinMetadata() == null) {
            pinsLoaded = true;
            pinTasks.add(firebase.fetchDroppedPins()
                    .addOnSuccessListener(pids -> Log.d(TAG, "Successfully fetched dropped pins."))
                    .addOnFailureListener(e -> Log.w(TAG, e)));
            pinTasks.add(firebase.fetchFoundPins()
                    .addOnSuccessListener(pids -> Log.d(TAG, "Successfully fetched found pins."))
                    .addOnFailureListener(e -> Log.w(TAG, e)));
            Tasks.whenAllComplete(pinTasks)
                    .addOnCompleteListener(pinFetchingComplete -> loadDiscoveredPins());
        }
        droppedMarkers = new ArrayList<>();
        friendMarkers = new ArrayList<>();
        nfcMarkers = new ArrayList<>();
        devMarkers = new ArrayList<>();
        generalMarkers = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        SupportMapFragment supportMapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        //noinspection ConstantConditions
        supportMapFragment.getMapAsync(googleMap -> {
            this.map = googleMap;
            styleMap();
            getDeviceLocation();
            if (!pinsLoaded) {
                loadDiscoveredPins();
            }
            map.setOnInfoWindowClickListener(marker -> {
                if (marker.getAlpha() == 1f) {
                    // Already discovered pin
                    //noinspection ConstantConditions
                    navController.navigate(
                            MapContainerFragmentDirections.pinView(marker.getTag().toString()));
                } else {
                    handleUndiscoveredPinClick(marker);
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
        pinnies_logo = requireView().findViewById(R.id.map_pinnies_logo);

        setPinnieCount();
        handleFilters();
    }

    private void handleFilters(){
        CheckBox generalBox = requireView().findViewById(R.id.checkbox_general);
        CheckBox friendBox = requireView().findViewById(R.id.checkbox_following);
        CheckBox nfcBox = requireView().findViewById(R.id.checkbox_nfc);
        CheckBox droppedBox = requireView().findViewById(R.id.checkbox_dropped);
        CheckBox pinpointBox = requireView().findViewById(R.id.checkbox_pinpoint);
        ImageView filterTab = requireView().findViewById(R.id.checkbox_tab);
        ConstraintLayout filterContainer = requireView().findViewById(R.id.filter_container);

        // Scale factor for setting padding in dp
        float scale = getResources().getDisplayMetrics().density;

        filterTab.setOnClickListener(view -> {
            if(isFilterVisible) {
                filterContainer.animate().translationX(0);
                isFilterVisible = false;
                filterTab.setImageResource(R.drawable.ic_back_arrow);
            }
            else {
                filterContainer.animate().translationX(-160 * scale + 0.5f);
                isFilterVisible = true;
                filterTab.setImageResource(R.drawable.ic_filter);
            }
        });

        generalBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for(Marker marker : generalMarkers){
                if(isChecked)
                    marker.setVisible(true);
                else marker.setVisible(false);
            }
        });
        friendBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for(Marker marker : friendMarkers){
                if(isChecked)
                    marker.setVisible(true);
                else marker.setVisible(false);
            }
        });
        nfcBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for(Marker marker : nfcMarkers){
                if(isChecked)
                    marker.setVisible(true);
                else marker.setVisible(false);
            }
        });
        droppedBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for(Marker marker : droppedMarkers){
                if(isChecked)
                    marker.setVisible(true);
                else marker.setVisible(false);
            }
        });
        pinpointBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for(Marker marker : devMarkers){
                if(isChecked)
                    marker.setVisible(true);
                else marker.setVisible(false);
            }
        });
    }

    private void styleMap() {
        if (getActivity() != null) {
            map.setInfoWindowAdapter(new InfoAdapter(requireContext()));
            int nightModeFlags = requireActivity().getResources()
                    .getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (nightModeFlags) {
                case Configuration.UI_MODE_NIGHT_YES:
                    map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),
                            R.raw.map_style_dark));
                    break;

                case Configuration.UI_MODE_NIGHT_NO:
                    map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),
                            R.raw.map_style_light));
                    break;

                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    break;
            }
        }
    }

    private void loadDiscoveredPins() {
        // Get dropped pins
        Iterator<PinMetadata> droppedIterator =
                firebase.getCachedDroppedPinMetadata().getIterator();
        while (droppedIterator.hasNext()) {
            String pinId = droppedIterator.next().getPinId();
            createDiscoveredPin(pinId, firebase.getCachedPin(pinId).getLocation(), PinSource.SELF);
        }
        // Get found pins
        Iterator<PinMetadata> foundIterator = firebase.getCachedFoundPinMetadata().getIterator();
        while (foundIterator.hasNext()) {
            PinMetadata pin = foundIterator.next();
            String pinId = pin.getPinId();
            PinSource pinSource = pin.getPinSource();
            createDiscoveredPin(pinId, firebase.getCachedPin(pinId).getLocation(), pinSource);
        }
    }

    private void createUndiscoveredPin(String key, Map<String, Object> val) {
        String authorUID = (String) val.get("authorUID");
        LatLng pinLocation = new LatLng((double) val.get("latitude"), (double) val.get("longitude"));
        float color = BitmapDescriptorFactory.HUE_RED;
        // Data field to persist in pin marker to know the pin source when the user finds the pin
        PinSource source = PinSource.GENERAL;
        ArrayList<Marker> markerList = generalMarkers;
        // TODO: change color for nfc pin (cyan)
        if(authorUID == "pinpoint"){
            color = BitmapDescriptorFactory.HUE_YELLOW;
            source = PinSource.DEV;
            markerList = devMarkers;
        }
        // Change color to green if user follows author.
        // Following is subject to change at each reload
        else if(firebase.getCachedFollowing(firebase.getUid()).contains(authorUID)) {
            color = BitmapDescriptorFactory.HUE_GREEN;
            source = PinSource.FRIEND;
            markerList = friendMarkers;
        }
        Marker pinMarker = map.addMarker(new MarkerOptions().icon(
                        BitmapDescriptorFactory.defaultMarker(color)).alpha(.3f)
                .position(pinLocation));
        pinMarker.setTag(key);
        pinMarker.setSnippet(source.name());
        markerList.add(pinMarker);
    }

    private void handleUndiscoveredPinClick(Marker marker) {
        if (getActivity() == null) {
            Toast.makeText(requireContext(), R.string.location_error_text, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        LocationDriver locationDriver = LocationDriver.getInstance(requireContext());
        locationDriver.getLastLocation(requireContext()).addOnCompleteListener(locationTask -> {
            Location userLoc = locationTask.getResult();
            if (!locationTask.isSuccessful() || userLoc == null) {
                Toast.makeText(requireContext(), R.string.location_error_text, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (LocationDriver.isCloseEnoughToFindPin(
                    new LatLng(userLoc.getLatitude(), userLoc.getLongitude()),
                    marker.getPosition())) {
                PinSource pinSource;
                switch (marker.getSnippet()){
                    case "DEV":
                        pinSource = PinSource.DEV;
                        break;
                    case "NFC":
                        pinSource = PinSource.NFC;
                        break;
                    default:
                        // Do not store friend enum in cloud,
                        // since friendship status is subject to change.
                        pinSource = PinSource.GENERAL;
                        break;
                }
                //noinspection ConstantConditions
                firebase.findPin((String) marker.getTag(), userLoc, pinSource).addOnSuccessListener(
                                pin -> navController.navigate(
                                        MapContainerFragmentDirections.pinView(marker.getTag().toString())))
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(),
                                Toast.LENGTH_LONG).show());
            } else {
                Toast.makeText(requireContext(), R.string.undiscovered_pin_not_close_enough,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createDiscoveredPin(String id, GeoPoint geoPoint, PinSource pinSource) {
        LatLng pinLocation = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
        float color;
        PinSource snippet = pinSource;
        ArrayList<Marker> markerList = generalMarkers;
        switch (pinSource){
            case DEV:
                color = BitmapDescriptorFactory.HUE_YELLOW;
                markerList = devMarkers;
                break;
            case SELF:
                color = BitmapDescriptorFactory.HUE_AZURE;
                markerList = droppedMarkers;
                break;
            case NFC:
                color = BitmapDescriptorFactory.HUE_CYAN;
                markerList = nfcMarkers;
                break;
            default:
                if(firebase.getCachedFollowing(firebase.getUid()).contains(firebase.getCachedPin(id).getAuthorUID())) {
                    color = BitmapDescriptorFactory.HUE_GREEN;
                    snippet = PinSource.FRIEND;
                    markerList = friendMarkers;
                }
                else color = BitmapDescriptorFactory.HUE_RED;
                break;
        }
        Marker pinMarker = map.addMarker(new MarkerOptions().icon(
                        BitmapDescriptorFactory.defaultMarker(color)).alpha(1f)
                .position(pinLocation));
        pinMarker.setTag(id);
        pinMarker.setSnippet(snippet.name());
        markerList.add(pinMarker);
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        map.setPadding(0, 0, 0, 150);
        if (getActivity() != null) {
            LocationDriver locationDriver = LocationDriver.getInstance(requireContext());
            Tasks.whenAllComplete(pinTasks).addOnCompleteListener(pinFetchingComplete -> {
                if (locationDriver.hasCoarseLocation(
                        requireContext()) && locationDriver.hasLocationOn(requireContext())) {
                    map.setMyLocationEnabled(true);
                    map.getUiSettings().setMyLocationButtonEnabled(true);
                    // Get location and nearby undiscovered pins
                    Task<Location> locationResult = locationDriver.getLastLocation(requireContext())
                            .addOnSuccessListener(location -> FirebaseDriver.getInstance()
                                    .fetchNearbyPins(location)
                                    .addOnSuccessListener(pins -> pins.forEach((key, val) -> {
                                        // load pin if undiscovered
                                        if (!firebase.getCachedDroppedPinMetadata().contains(
                                                key) && !firebase.getCachedFoundPinMetadata()
                                                .contains(key)) createUndiscoveredPin(key, val);
                                    })));
                    locationResult.addOnCompleteListener(requireActivity(), task -> {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            Location lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), 16));
                            }
                        }
                    });
                }
            });
        }
    }

    private void setPinnieCount() {
        if (firebase.getCachedPinnies() != null) {
            pinnieCount = firebase.getCachedPinnies();
            setPinniesUI();
            return;
        }

        firebase.getPinnies().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                pinnieCount = task.getResult();
                Log.d(TAG, String.format("Got %s pinnies for user", pinnieCount.toString()));
                setPinniesUI();
            } else {
                Log.d(TAG, "get failed with ", task.getException());
            }
        });
    }

    private void setPinniesUI() {
        pinniesText.setText(FormatUtils.humanReadablePinnies(pinnieCount));
        pinnieProgressBar.setVisibility(View.GONE);
        pinniesText.setVisibility(View.VISIBLE);
        pinnies_logo.setVisibility(View.VISIBLE);
    }
}