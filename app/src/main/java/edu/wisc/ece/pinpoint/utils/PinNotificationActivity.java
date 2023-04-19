package edu.wisc.ece.pinpoint.utils;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;

public class PinNotificationActivity extends Worker {

    FirebaseDriver firebaseDriver;
    Map<String, Map<String, Object>> nearbyPins;
    Context context;
    String x;
    private Location loc;


    public PinNotificationActivity(@NonNull Context context,
                                   @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            LocationDriver locationDriver = LocationDriver.getInstance(context);

            if (locationDriver.hasCoarseLocation(context))
                locationDriver.getLastLocation(context).addOnCompleteListener(task -> {
                    loc = task.getResult();
                    if (loc != null) {
                        firebaseDriver = FirebaseDriver.getInstance();
                        firebaseDriver.fetchNearbyPins(loc).addOnCompleteListener(task1 -> {
                            nearbyPins = task1.getResult();
                            int i = nearbyPins.size();
                            x = String.valueOf(i);
                            NotificationDriver notificationDriver;
                            notificationDriver = NotificationDriver.getInstance(context);
                            notificationDriver.updatePersistent("Pins", x + " pins found nearby");
                        });
                    }

                });
        }, 1000);

        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }
}
