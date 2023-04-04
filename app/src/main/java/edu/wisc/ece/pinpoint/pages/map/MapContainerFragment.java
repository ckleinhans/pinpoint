package edu.wisc.ece.pinpoint.pages.map;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.utils.LocationDriver;

public class MapContainerFragment extends Fragment {
    private ActivityResultLauncher<String[]> locationPermissionRequest;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.map_layout, new MapFragment(), "MapFragment")
                .commit();
        // Code for requesting location
        locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            Boolean coarseLocationGranted =
                                    result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION,
                                            false);
                            if (coarseLocationGranted != null && coarseLocationGranted) {
                                Fragment mapFragment =
                                        getChildFragmentManager().findFragmentByTag("MapFragment");
                                getChildFragmentManager().beginTransaction().detach(mapFragment)
                                        .commit();
                                getChildFragmentManager().beginTransaction().attach(mapFragment)
                                        .commit();
                            }
                        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_container, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION});
        NavController navController = Navigation.findNavController(view);
        FloatingActionButton newPinButton = requireView().findViewById(R.id.newPinButton);
        newPinButton.setOnClickListener((buttonView) -> {
            if (LocationDriver.getInstance(requireActivity()).hasFineLocation(requireContext())
                    & LocationDriver.getInstance(requireActivity())
                    .hasLocationOn(requireContext())) {
                navController.navigate(MapContainerFragmentDirections.newPin());
            }
            else {
                Toast.makeText(requireContext(),
                        "PinPoint needs precise location permissions to drop pins.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}