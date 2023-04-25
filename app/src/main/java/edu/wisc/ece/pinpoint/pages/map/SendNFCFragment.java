package edu.wisc.ece.pinpoint.pages.map;

import android.Manifest;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class SendNFCFragment extends Fragment {

    private final FirebaseDriver firebase = FirebaseDriver.getInstance();
    private final String sender = firebase.getCachedUser(firebase.getUid()).getUsername();
    private TextView progressText;
    private TextView recipientText;
    private String pid;
    private NavController navController;

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    Nearby.getConnectionsClient(requireContext()).acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            recipientText.setText("Connected to "+endpointId);
                            Payload bytesPayload = Payload.fromBytes(createByteArray());
                            Nearby.getConnectionsClient(requireContext()).sendPayload(endpointId, bytesPayload);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            break;
                        default:
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
                    progressText.setText("TRANSFERRING!");
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                    if(payloadTransferUpdate.getStatus() == PayloadTransferUpdate.Status.SUCCESS){
                        progressText.setText("TRANSFER COMPLETE!");
                        // Pop the original post & the share fragment from the navigation stack
                        navController.popBackStack();
                        navController.popBackStack();
                        navController.navigate(edu.wisc.ece.pinpoint.NavigationDirections.pinView(pid));
                    }
                }
            };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_send_nfc, container, false);
        progressText = view.findViewById(R.id.send_text);
        recipientText = view.findViewById(R.id.recipient_name);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Fetch pin data & load using argument
        Bundle args = getArguments();
        pid = args != null ? SendNFCFragmentArgs.fromBundle(args).getPid() : null;
        navController = Navigation.findNavController(view);
        checkPermissions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // End connection & stop advertising
        Nearby.getConnectionsClient(requireContext()).stopAllEndpoints();
        Nearby.getConnectionsClient(requireContext()).stopAdvertising();
    }

    private void checkPermissions(){
        // Code for requesting nearby share
        ActivityResultLauncher<String[]> nearbyPermissions =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            Boolean nearbyDevicesGranted =
                                    result.getOrDefault(Manifest.permission.BLUETOOTH_ADVERTISE,
                                            false);
                            if (nearbyDevicesGranted != null && nearbyDevicesGranted) {
                                startAdvertising();
                            }
                            else Toast.makeText(requireContext(),
                                    "Sharing permissions not granted.", Toast.LENGTH_LONG).show();
                        });
        nearbyPermissions.launch(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.NEARBY_WIFI_DEVICES});
    }


    private void startAdvertising() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();
        Nearby.getConnectionsClient(requireContext())
                .startAdvertising(
                        sender, getString(R.string.service_id), connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're discovering!
                            progressText.setText("ADVERTISING!");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We're unable to start discovering.
                            progressText.setText("not advertising!");
                        });
    }

    private byte[] createByteArray() {
        Pin pin = firebase.getCachedPin(pid);
        Map<String, Object> pinData = new HashMap<>();
        pinData.put("authorUID", pin.getAuthorUID());
        pinData.put("latitude", pin.getLocation().getLatitude());
        pinData.put("longitude", pin.getLocation().getLongitude());
        pinData.put("pid", pid);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        byte[] bytes = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(pinData);
            out.flush();
            bytes = bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return bytes;
    }
}