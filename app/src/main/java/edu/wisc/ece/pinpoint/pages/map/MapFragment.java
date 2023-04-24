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
    private Long pinnieCount;
    private ProgressBar pinnieProgressBar;
    private ImageView pinnies_logo;
    private TextView pinniesText;
    private ArrayList<Marker> droppedMarkers;
    private ArrayList<Marker> friendMarkers;
    private ArrayList<Marker> nfcMarkers;
    private ArrayList<Marker> devMarkers;
    private ArrayList<Marker> strangerMarkers;
    private boolean isFilterVisible = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        droppedMarkers = new ArrayList<>();
        friendMarkers = new ArrayList<>();
        nfcMarkers = new ArrayList<>();
        devMarkers = new ArrayList<>();
        strangerMarkers = new ArrayList<>();
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
            map.clear();
            styleMap();
            getDeviceLocation();
            loadDiscoveredPins();
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

    private void handleFilters() {
        CheckBox strangerBox = requireView().findViewById(R.id.checkbox_strangers);
        CheckBox friendBox = requireView().findViewById(R.id.checkbox_friends);
        CheckBox nfcBox = requireView().findViewById(R.id.checkbox_nfc);
        CheckBox droppedBox = requireView().findViewById(R.id.checkbox_dropped);
        CheckBox devBox = requireView().findViewById(R.id.checkbox_devs);
        ImageView filterTab = requireView().findViewById(R.id.checkbox_tab);
        ConstraintLayout filterContainer = requireView().findViewById(R.id.filter_container);

        // Scale factor for setting padding in dp
        float scale = getResources().getDisplayMetrics().density;

        filterTab.setOnClickListener(view -> {
            if (isFilterVisible) {
                filterContainer.animate().translationX(0);
                isFilterVisible = false;
                filterTab.setImageResource(R.drawable.ic_back_arrow);
            } else {
                filterContainer.animate().translationX(-141 * scale + 0.5f);
                isFilterVisible = true;
                filterTab.setImageResource(R.drawable.ic_filter);
            }
        });

        strangerBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> strangerMarkers.forEach(m -> m.setVisible(isChecked)));
        friendBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> friendMarkers.forEach(m -> m.setVisible(isChecked)));
        nfcBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> nfcMarkers.forEach(m -> m.setVisible(isChecked)));
        droppedBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> droppedMarkers.forEach(m -> m.setVisible(isChecked)));
        devBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> devMarkers.forEach(m -> m.setVisible(isChecked)));
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
        //noinspection ConstantConditions
        LatLng pinLocation =
                new LatLng((double) val.get("latitude"), (double) val.get("longitude"));
        float color = BitmapDescriptorFactory.HUE_RED;
        // Data field to persist in pin marker to know the pin source when the user finds the pin
        PinSource source = PinSource.GENERAL;
        ArrayList<Marker> markerList = strangerMarkers;
        // TODO: change color for nfc pin (cyan)
        if ("pinpoint".equals(authorUID)) {
            color = BitmapDescriptorFactory.HUE_YELLOW;
            source = PinSource.DEV;
            markerList = devMarkers;
        }
        // Change color to green if user follows author.
        // Following is subject to change at each reload
        else if (firebase.getCachedFollowing(firebase.getUid()).contains(authorUID)) {
            color = BitmapDescriptorFactory.HUE_GREEN;
            source = PinSource.FRIEND;
            markerList = friendMarkers;
        }
        Marker pinMarker = map.addMarker(
                new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(color)).alpha(.3f)
                        .position(pinLocation));
        //noinspection ConstantConditions
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
                //noinspection ConstantConditions
                switch (marker.getSnippet()) {
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
                firebase.findPin((String) marker.getTag(), userLoc, pinSource)
                        .addOnSuccessListener(reward -> {
                            Toast.makeText(requireContext(),
                                    String.format(getString(R.string.pinnie_reward_message), reward),
                                    Toast.LENGTH_LONG).show();
                            //noinspection ConstantConditions
                            navController.navigate(MapContainerFragmentDirections.pinView(
                                    marker.getTag().toString()));
                        }).addOnFailureListener(
                                e -> Toast.makeText(requireContext(), e.getMessage(),
                                                Toast.LENGTH_LONG)
                                        .show());
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
        ArrayList<Marker> markerList = strangerMarkers;
        switch (pinSource) {
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
                if (firebase.getCachedFollowing(firebase.getUid())
                        .contains(firebase.getCachedPin(id).getAuthorUID())) {
                    color = BitmapDescriptorFactory.HUE_GREEN;
                    snippet = PinSource.FRIEND;
                    markerList = friendMarkers;
                } else color = BitmapDescriptorFactory.HUE_RED;
                break;
        }
        Marker pinMarker = map.addMarker(
                new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(color)).alpha(1f)
                        .position(pinLocation));
        //noinspection ConstantConditions
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
