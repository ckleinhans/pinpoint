package edu.wisc.ece.pinpoint.utils;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Task;

import java.util.Map;

public class PinNotificationActivity extends Worker {

    Button button;

    private Task<Location> location;
    private Location loc;
    private Task<Location> lastLocation;
    double lat = 5.0;
    Object l;

    FirebaseDriver firebaseDriver;
    Map<String, Object> nearbyPins;

    NotificationDriver notificationDriver;

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

               location =  locationDriver.getCurrentLocation(context);
               lastLocation = locationDriver.getLastLocation(context);

                locationDriver.getCurrentLocation(context).addOnCompleteListener(task -> {
                   loc = task.getResult();
                   if(loc != null){
                   firebaseDriver = FirebaseDriver.getInstance();
                   firebaseDriver.fetchNearbyPins(loc).addOnCompleteListener(task1 -> {
                       nearbyPins = task1.getResult();
                       int i = nearbyPins.size();
                       x = String.valueOf(i);
                       notificationDriver = NotificationDriver.getInstance(context);
                       notificationDriver.updatePersistent("Pins", x + " pins found nearby");
                   });
                   }
                   else{
                        notificationDriver = NotificationDriver.getInstance(context);
                        notificationDriver.updatePersistent("Location Access disabled", "no pins");
                   }
                });
            }
        }, 1000);

        // Indicate whether the work finished successfully with the Result
        return Result.retry();
    }
            }





}