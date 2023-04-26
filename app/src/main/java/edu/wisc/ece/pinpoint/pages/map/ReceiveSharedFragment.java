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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class ReceiveSharedFragment extends Fragment {

    private final FirebaseDriver firebase = FirebaseDriver.getInstance();
    private final String recipient = firebase.getUid();
    private TextView progressText;
    private TextView senderText;
    private NavController navController;
    private static final String TAG = "RECEIVE";

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
            // An endpoint was found. We request a connection to it.
            Nearby.getConnectionsClient(requireContext())
                    .requestConnection(recipient, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener(
                            (Void unused) -> {
                                // We successfully requested a connection. Now both sides
                                // must accept before the connection is established.
                            })
                    .addOnFailureListener(
                            (Exception e) -> {
                                // Nearby Connections failed to request the connection.
                                Toast.makeText(requireContext(),
                                        R.string.pin_share_exception_text, Toast.LENGTH_LONG).show();
                            });
        }

        @Override
        public void onEndpointLost(String endpointId) {
            // A previously discovered endpoint has gone away.
        }
    };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                private String sender;
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.

                    Nearby.getConnectionsClient(requireContext()).acceptConnection(endpointId, payloadCallback);
                    sender = connectionInfo.getEndpointName();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            senderText.setText("Connected to "+sender);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            Toast.makeText(requireContext(),
                                    R.string.connection_rejected_text, Toast.LENGTH_LONG).show();
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            Toast.makeText(requireContext(),
                                    R.string.pin_share_exception_text, Toast.LENGTH_LONG).show();
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
                    storePinData(payload.asBytes());
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                    switch(payloadTransferUpdate.getStatus()){
                        case PayloadTransferUpdate.Status.SUCCESS:
                            progressText.setText("TRANSFER COMPLETE!");
                            break;
                        default:
                            Toast.makeText(requireContext(),
                                    R.string.pin_share_exception_text, Toast.LENGTH_LONG).show();
                            break;
                    }

                }
            };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receive_nfc, container, false);
        progressText = view.findViewById(R.id.receive_text);
        senderText = view.findViewById(R.id.sender_name);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        checkPermissions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // End connection & stop discovering
        Nearby.getConnectionsClient(requireContext()).stopAllEndpoints();
        Nearby.getConnectionsClient(requireContext()).stopDiscovery();
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
                                startDiscovery();
                            }
                            else Toast.makeText(requireContext(),
                                    "Sharing permissions not granted.", Toast.LENGTH_LONG).show();
                });
        nearbyPermissions.launch(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.NEARBY_WIFI_DEVICES});
    }

    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();
        Nearby.getConnectionsClient(requireContext())
                .startDiscovery(getString(R.string.service_id), endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're discovering!
                            progressText.setText("DISCOVERING!");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We're unable to start discovering.
                            progressText.setText("not discovering!");
                        });
    }

    private void storePinData(byte[] bytes) {
        // Parse pin data from byte array
        Map<String, Object> pinData;
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput objIn = null;
        try {
            objIn = new ObjectInputStream(bis);
            pinData = (Map<String, Object>) objIn.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (objIn != null) {
                    objIn.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        // If pin is not null, the data was successfully parsed. Store in pin file
        if (pinData != null){
            String pid = (String) pinData.get("pid");
            // If the user already has this pin, pop this from the stack and nav to pin view
            if(firebase.getCachedDroppedPinMetadata().contains(pid) ||
                    firebase.getCachedFoundPinMetadata().contains(pid)){
                navController.popBackStack();
                navController.navigate(edu.wisc.ece.pinpoint.NavigationDirections.pinView(pid));
            } else {
                // If the user doesn't have the pin, create one
                // Parse current shared pins file
                String filename = requireContext().getFilesDir() + "shared_pins";
                HashMap<String, Map<String, Object>> nfcPins = new HashMap<>();
                try {
                    FileInputStream fis = new FileInputStream(filename);
                    ObjectInputStream in = new ObjectInputStream(fis);
                    nfcPins = (HashMap<String, Map<String, Object>>) in.readObject();
                    in.close();
                    fis.close();
                } catch (FileNotFoundException e) {
                    Log.i(TAG, "No existing nfc pins");
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
                // Add new pin to hash map
                nfcPins.put(pid, pinData);
                try {
                    FileOutputStream fos = new FileOutputStream(filename);
                    ObjectOutputStream out = new ObjectOutputStream(fos);
                    out.writeObject(nfcPins);
                    out.close();
                    fos.close();
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
                navController.navigate(edu.wisc.ece.pinpoint.NavigationDirections.map());
            }
        }

    }


}