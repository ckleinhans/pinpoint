package edu.wisc.ece.pinpoint.pages.map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
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
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.NearbyPinData;
import edu.wisc.ece.pinpoint.data.OrderedPinMetadata;
import edu.wisc.ece.pinpoint.data.PinMetadata;
import edu.wisc.ece.pinpoint.data.PinMetadata.PinSource;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;
import edu.wisc.ece.pinpoint.utils.LocationDriver;

public class MapFragment extends Fragment {
    private static final String TAG = MapFragment.class.getName();
    private final FirebaseDriver firebase = FirebaseDriver.getInstance();
    private final List<Task<OrderedPinMetadata>> pinTasks = new ArrayList<>();
    private final Integer MARKER_IMAGE_LOAD_TIME = 250;
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
    private HashMap<String, Map<String, Object>> sharedPins;
    private ConstraintLayout loadLayoutContainer;
    private Button directionsButton;
    // Scale factor for setting padding in dp
    private float scale;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        droppedMarkers = new ArrayList<>();
        friendMarkers = new ArrayList<>();
        nfcMarkers = new ArrayList<>();
        devMarkers = new ArrayList<>();
        strangerMarkers = new ArrayList<>();
        sharedPins = new HashMap<>();
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
            setMarkerWindowFunction();
            loadSharedPins();
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
        //noinspection ConstantConditions
        loadLayoutContainer =
                this.getParentFragment().requireView().findViewById(R.id.map_load_layout_container);
        pinnieProgressBar = requireView().findViewById(R.id.map_pinnies_progress);
        pinniesText = requireView().findViewById(R.id.map_pinnies_text);
        pinnies_logo = requireView().findViewById(R.id.map_pinnies_logo);
        directionsButton = requireView().findViewById(R.id.map_directions_button);
        scale = getResources().getDisplayMetrics().density;

        setPinnieCount();
        handleFilters();

        ImageButton nfc_button = requireView().findViewById(R.id.map_nfc_share);
        nfc_button.setOnClickListener(
                v -> navController.navigate(MapContainerFragmentDirections.receiveNFCPin()));
    }

    private void lockUI() {
        loadLayoutContainer.setVisibility(View.VISIBLE);
    }

    private void restoreUI() {
        loadLayoutContainer.setVisibility(View.GONE);
    }

    private void handleFilters() {
        CheckBox strangerBox = requireView().findViewById(R.id.checkbox_strangers);
        CheckBox friendBox = requireView().findViewById(R.id.checkbox_friends);
        CheckBox nfcBox = requireView().findViewById(R.id.checkbox_nfc);
        CheckBox droppedBox = requireView().findViewById(R.id.checkbox_dropped);
        CheckBox devBox = requireView().findViewById(R.id.checkbox_devs);
        ImageView filterTab = requireView().findViewById(R.id.checkbox_tab);
        ConstraintLayout filterContainer = requireView().findViewById(R.id.filter_container);


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

    private void setMarkerWindowFunction() {
        if (getActivity() != null) {
            map.setInfoWindowAdapter(new InfoAdapter(requireContext()));
            map.setOnMarkerClickListener(mark -> {
                // Enable directions button
                directionsButton.setClickable(true);
                // Show directions button
                directionsButton.animate().translationX(-12 * scale + 0.5f);
                // Get coordinates of selected pin
                LatLng latLng = mark.getPosition();
                // Move map camera to focus on clicked pin
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                String location =
                        "geo:" + latLng.latitude + "," + latLng.longitude + "?z=17&q=" + latLng.latitude + "," + latLng.longitude;
                // Open Google Maps at pin coordinates
                directionsButton.setOnClickListener(view -> {
                    Uri gmmIntentUri = Uri.parse(location);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                });
                map.setOnInfoWindowCloseListener(marker -> {
                    // Disable clicking button while it is sliding away
                    directionsButton.setClickable(false);
                    // Hide directions button
                    directionsButton.animate().translationX(171 * scale + 0.5f);
                });
                // Call once to force an image load
                mark.showInfoWindow();

                // Call a second time to set the image (should actually be loaded by this time)
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(mark::showInfoWindow, MARKER_IMAGE_LOAD_TIME);
                return true;
            });
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

    private void loadSharedPins() {
        // Pull hash map of <String pid, Map<String, Object> data>
        // Drop an undiscovered marker for each
        if (getActivity() != null) {
            // Create trace to track shared pin load time
            Trace trace = FirebasePerformance.getInstance().newTrace("loadSharedPins");
            trace.start();
            String filename = requireContext().getFilesDir() + "shared_pins";
            try {
                FileInputStream fis = new FileInputStream(filename);
                ObjectInputStream in = new ObjectInputStream(fis);
                //noinspection unchecked
                sharedPins = (HashMap<String, Map<String, Object>>) in.readObject();
                sharedPins.forEach((pid, data) -> {
                    NearbyPinData pinData = new NearbyPinData(data);
                    pinData.setSource(PinSource.NFC);
                    createUndiscoveredPin(pid, pinData);
                });
                in.close();
                fis.close();
                Log.d(TAG, "Successfully loaded " + sharedPins.size() + " shared pins");
            }
            // If file not found, the user has no NFC pins
            catch (FileNotFoundException e) {
                Log.d(TAG, "No shared pins loaded.");
                // Reset nfc pins if exception occurs
                sharedPins = new HashMap<>();
            } catch (Exception e) {
                String message = "Error loading shared pins";
                Log.w(TAG, message, e);
                FirebaseCrashlytics.getInstance().setCustomKey("message", message);
                FirebaseCrashlytics.getInstance().recordException(e);
                // Reset nfc pins if exception occurs
                sharedPins = new HashMap<>();
            }
            trace.stop();
        }
    }

    private void updateSharedPins() {
        // Rewrite nfc_pins file with updated hash map
        String filename = requireContext().getFilesDir() + "shared_pins";
        // Create trace to track shared pin load time
        Trace trace = FirebasePerformance.getInstance().newTrace("updateSharedPins");
        trace.start();
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(sharedPins);
            out.close();
            fos.close();
            Log.d(TAG, "Successfully stored " + sharedPins.size() + " pins.");
        } catch (Exception e) {
            String message = "Error updating stored shared pins";
            Log.w(TAG, message, e);
            FirebaseCrashlytics.getInstance().setCustomKey("message", message);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        trace.stop();
    }

    private void createUndiscoveredPin(String key, NearbyPinData val) {
        float color = BitmapDescriptorFactory.HUE_RED;
        // Data field to persist in pin marker to know the pin source when the user finds the pin
        ArrayList<Marker> markerList = strangerMarkers;
        if (val.getSource() == PinSource.NFC) {
            color = BitmapDescriptorFactory.HUE_CYAN;
            markerList = nfcMarkers;
        }
        if (val.getSource() == PinSource.DEV) {
            color = BitmapDescriptorFactory.HUE_YELLOW;
            markerList = devMarkers;
        }
        // Change color to green if user follows author.
        // Following is subject to change at each reload
        else if (firebase.getCachedFollowing(firebase.getUid()).contains(val.getAuthorUID())) {
            color = BitmapDescriptorFactory.HUE_GREEN;
            val.setSource(PinSource.FRIEND);
            markerList = friendMarkers;
        }
        Marker pinMarker = map.addMarker(
                new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(color)).alpha(.3f)
                        .position(val.getLocation()));
        //noinspection ConstantConditions
        pinMarker.setTag(key);
        pinMarker.setSnippet(val.getSource().name());
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
                lockUI();
                String pinId = (String) marker.getTag();
                NearbyPinData pinData = firebase.getCachedNearbyPin(pinId);
                PinSource source = (sharedPins.containsKey(pinId)) ? PinSource.NFC :
                        pinData.getSource() == PinSource.FRIEND ? PinSource.GENERAL :
                                pinData.getSource();
                firebase.findPin(pinId, userLoc, source).addOnSuccessListener(reward -> {
                    restoreUI();
                    if (source.equals(PinSource.NFC)) {
                        sharedPins.remove(pinId);
                        updateSharedPins();
                    }
                    Toast.makeText(requireContext(),
                            String.format(getString(R.string.pinnie_reward_message), reward),
                            Toast.LENGTH_LONG).show();
                    //noinspection ConstantConditions
                    navController.navigate(
                            MapContainerFragmentDirections.pinView(marker.getTag().toString()));
                }).addOnFailureListener(e -> {
                    restoreUI();
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
                                                .contains(key) && !sharedPins.containsKey(key))
                                            createUndiscoveredPin(key, val);
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

        firebase.fetchPinnies().addOnSuccessListener(pinnies -> {
            pinnieCount = pinnies;
            setPinniesUI();
        });
    }

    private void setPinniesUI() {
        pinniesText.setText(FormatUtils.trimmedNumber(pinnieCount));
        pinnieProgressBar.setVisibility(View.GONE);
        pinniesText.setVisibility(View.VISIBLE);
        pinnies_logo.setVisibility(View.VISIBLE);
    }
}
