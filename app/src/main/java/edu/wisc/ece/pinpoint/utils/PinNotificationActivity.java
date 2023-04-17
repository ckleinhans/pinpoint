package edu.wisc.ece.pinpoint.utils;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import java.util.Map;

public class PinNotificationActivity extends Worker {

    private Location loc;
    FirebaseDriver firebaseDriver;
    Map<String, Object> nearbyPins;

    Context context;
    String x;


    public PinNotificationActivity(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;


    }

    @NonNull
    @Override
    public Result doWork() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                LocationDriver locationDriver = LocationDriver.getInstance(context);

                locationDriver.getLastLocation(context).addOnCompleteListener(task -> {
                   loc = task.getResult();
                   if(loc != null){
                   firebaseDriver = FirebaseDriver.getInstance();
                   firebaseDriver.fetchNearbyPins(loc).addOnCompleteListener(task1 -> {
                       nearbyPins = task1.getResult();
                       int i = nearbyPins.size();
                       x = String.valueOf(i);
                       NotificationDriver notificationDriver;
                       notificationDriver = NotificationDriver.getInstance(context);
                       notificationDriver.sendOneShot("Pins", x + " pins found nearby");
                   });
                   }

                });
            }
        }, 1000);

        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }
            }
