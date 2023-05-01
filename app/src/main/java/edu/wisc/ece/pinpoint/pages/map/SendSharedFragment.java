package edu.wisc.ece.pinpoint.pages.map;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class SendSharedFragment extends Fragment {
    private final FirebaseDriver firebase = FirebaseDriver.getInstance();
    private final String sender = firebase.getCachedUser(firebase.getUid()).getUsername();
    private TextView progressText;
    private TextView recipientText;
    private String pid;
    private NavController navController;
    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            progressText.setText(R.string.share_in_progress_text);
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s,
                                            @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            if (payloadTransferUpdate.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                progressText.setText(R.string.share_complete_text);
                // Pop the original post & the share fragment from the navigation stack
                navController.popBackStack();
            }
        }
    };
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                private String recipient;

                @Override
                public void onConnectionInitiated(@NonNull String endpointId,
                                                  ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    Nearby.getConnectionsClient(requireContext())
                            .acceptConnection(endpointId, payloadCallback);
                    recipient = connectionInfo.getEndpointName();
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId,
                                               ConnectionResolution result) {
                    if (result.getStatus().getStatusCode() == ConnectionsStatusCodes.STATUS_OK) {
                        // We're connected! Can now start sending and receiving data.
                        String connection = "Connected to " + recipient;
                        recipientText.setText(connection);
                        Payload bytesPayload = Payload.fromBytes(createByteArray());
                        Nearby.getConnectionsClient(requireContext())
                                .sendPayload(endpointId, bytesPayload);
                    } else {
                        Toast.makeText(requireContext(), R.string.pin_share_exception_text,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_send_nfc, container, false);
        progressText = view.findViewById(R.id.send_text);
        recipientText = view.findViewById(R.id.recipient_name);
        ImageButton backButton = view.findViewById(R.id.share_pin_back_button);
        backButton.setOnClickListener((v) -> navController.popBackStack());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Fetch pin data & load using argument
        Bundle args = getArguments();
        pid = args != null ? SendSharedFragmentArgs.fromBundle(args).getPid() : null;
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

    private void checkPermissions() {
        // Code for requesting nearby share
        ActivityResultLauncher<String[]> nearbyPermissions =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            Boolean nearbyDevicesGranted =
                                    result.getOrDefault(Manifest.permission.BLUETOOTH_ADVERTISE,
                                            false);
                            if (nearbyDevicesGranted != null && nearbyDevicesGranted) {
                                startAdvertising();
                            } else
                                Toast.makeText(requireContext(), "Sharing permissions not granted.",
                                        Toast.LENGTH_LONG).show();
                        });
        nearbyPermissions.launch(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.NEARBY_WIFI_DEVICES});
    }


    private void startAdvertising() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();
        Nearby.getConnectionsClient(requireContext())
                .startAdvertising(sender, getString(R.string.service_id),
                        connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener((Void unused) -> {
                    // We're discovering!
                    progressText.setText(R.string.searching_for_recipient_text);
                }).addOnFailureListener((Exception e) -> {
                    // We're unable to start discovering.
                    FirebaseCrashlytics.getInstance()
                            .setCustomKey("message", "Error trying to advertise pin");
                    FirebaseCrashlytics.getInstance().recordException(e);
                    progressText.setText(R.string.pin_share_exception_text);
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
        ObjectOutputStream out;
        byte[] bytes;
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