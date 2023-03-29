package edu.wisc.ece.pinpoint.utils;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import edu.wisc.ece.pinpoint.MainActivity;
import edu.wisc.ece.pinpoint.R;

/**
 * So this class is kinda jank cause we need one instance of it shared across the entire app.
 * The way I'm accomplishing this now is to have a static instance of this class within itself, so
 * we populate the instance once in MainActivity, and then just call NotificationDriver.getInstance(null)
 * to get the same instance anywhere in the app. Essentially anytime you want to send notifications
 * in the app, just run e.g.
 * <pre>
 *   NotificationDriver
 *        .getInstance(null)
 *        .sendOneShot("title", "contents");
 * </pre>
 */
public class NotificationDriver {
     public static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
     private final Context context;
     private static NotificationDriver instance;
     private NotificationManager manager;
     private final String PERSISTENT_CHANNEL_ID = "pinpointPersistentChannel";
     private final String ONESHOT_CHANNEL_ID = "pinpointOneshotChannel";
     private int idCounter;
     private int persistentID;
     private String persistentTitle = "Uninitialized title!";

     private NotificationDriver(Context context) {
          this.context = context;
          this.manager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
          this.idCounter = 0;
          this.persistentID = -1;
          createNotificationChannel(this.PERSISTENT_CHANNEL_ID, "Persistent Pin Updates");
          createNotificationChannel(this.ONESHOT_CHANNEL_ID, "Misc PinPoint Notifications");
     }

     public static NotificationDriver getInstance(Context context) {
          // if we haven't instantiated a NotificationDriver yet, do so
          if (instance == null) {
               instance = new NotificationDriver(context);
          } else {
               // otherwise, make sure we have permissions before returning
               instance.checkPermissions();
               // TODO: seems like the first notification sent right after accepting permissions doesn't show up
          }
          return instance;
     }

     public void checkPermissions() {
          // as of android 13, we need to explicitly ask for notification permissions
          if (ContextCompat.checkSelfPermission(this.context, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions((Activity) this.context, new String[]{ POST_NOTIFICATIONS }, 0);
          }
     }

     public void updatePersistent(String content) {
          updatePersistent(this.persistentTitle, content);
     }
     public void updatePersistent(String title, String content) {
          // if we haven't made a persistent notification yet, create a new one
          if (this.persistentID == -1) {
               this.persistentID = sendNotification(title, content, this.PERSISTENT_CHANNEL_ID);
          } else {
               sendNotification(title, content, this.PERSISTENT_CHANNEL_ID, this.persistentID);
          }
     }

     public void sendOneShot(String title, String content) {
          sendNotification(title, content, this.ONESHOT_CHANNEL_ID);
     }

     private int sendNotification(String title, String content, String channelID) {
          int ID = this.idCounter++;
          sendNotification(title, content, channelID, ID);
          return ID;
     }

     private void sendNotification(String title, String content, String channelID, int notifID) {
          Intent notificationIntent = new Intent(this.context, MainActivity.class);
          PendingIntent notificationPendingIntent = PendingIntent.getActivity(this.context,
                  0, notificationIntent,
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

          NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, channelID)
                  .setSmallIcon(R.drawable.ic_notif_icon_small)
                  .setContentTitle(title)
                  .setContentText(content)
                  .setContentIntent(notificationPendingIntent)
                  .setAutoCancel(true)
                  .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                  .setDefaults(NotificationCompat.DEFAULT_ALL);

          this.manager.notify(notifID, builder.build());
     }

     private void createNotificationChannel(String channelID, String channelName) {
          int importance = NotificationManager.IMPORTANCE_DEFAULT;
          NotificationChannel channel = new NotificationChannel(channelID, channelName, importance);
          this.manager.createNotificationChannel(channel);
     }

}
