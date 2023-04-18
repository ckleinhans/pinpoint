package edu.wisc.ece.pinpoint.utils;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.ActivityItem;
import edu.wisc.ece.pinpoint.data.ActivityList;
import edu.wisc.ece.pinpoint.data.GlideApp;
import edu.wisc.ece.pinpoint.data.OrderedPinMetadata;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.PinMetadata;
import edu.wisc.ece.pinpoint.data.User;

public class FirebaseDriver {
    private static final String TAG = FirebaseDriver.class.getName();
    private static FirebaseDriver instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseFunctions functions;
    private final Map<String, User> users;
    private final Map<String, Pin> pins;
    private final Map<String, OrderedPinMetadata> userPinMetadata;
    private final HashMap<String, ActivityList> activityMap;
    private final HashMap<String, HashSet<String>> userFollowerIds;
    private final HashMap<String, HashSet<String>> userFollowingIds;
    private OrderedPinMetadata foundPinMetadata;
    private OrderedPinMetadata droppedPinMetadata;
    private Long pinnies;

    private FirebaseDriver() {
        if (instance != null) {
            throw new IllegalStateException("FirebaseDriver has already been instantiated.");
        }
        instance = this;
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        functions = FirebaseFunctions.getInstance();
        users = new HashMap<>();
        pins = new HashMap<>();
        userPinMetadata = new HashMap<>();
        activityMap = new HashMap<>();
        userFollowerIds = new HashMap<>();
        userFollowingIds = new HashMap<>();
    }

    public static FirebaseDriver getInstance() {
        if (instance == null) {
            new FirebaseDriver();
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void sendEmailVerification(@Nullable OnCompleteListener<Void> onComplete) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Cannot send email verification when not logged in.");
        }
        Task<Void> sendEmailTask =
                user.sendEmailVerification().addOnFailureListener(e -> Log.w(TAG, e));
        if (onComplete != null) {
            sendEmailTask.addOnCompleteListener(onComplete);
        }
    }

    public boolean isVerified() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Cannot verify user when not logged in.");
        }
        return !user.getProviderData().get(1).getProviderId()
                .equals("password") || user.isEmailVerified();
    }

    public Task<Void> reloadAuth() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Cannot reload authentication when not logged in.");
        }
        return auth.getCurrentUser().reload();
    }

    public Task<Void> logout(@NonNull Context context) {
        foundPinMetadata = null;
        droppedPinMetadata = null;
        return AuthUI.getInstance().signOut(context);
    }

    public String getUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public boolean isNewUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("User must be logged in to check if they are new.");
        }
        FirebaseUserMetadata metadata = auth.getCurrentUser().getMetadata();
        //noinspection ConstantConditions
        return metadata.getLastSignInTimestamp() - metadata.getCreationTimestamp() < 1000;
    }

    public Task<User> fetchUser(@NonNull String uid) {
        return db.collection("users").document(uid).get().continueWith(task -> {
            User user = task.getResult().toObject(User.class);
            users.put(uid, user);
            return user;
        }).addOnFailureListener(e -> Log.w(TAG, String.format("Error fetching user %s", uid), e));
    }

    public User getCachedUser(@NonNull String uid) {
        return users.get(uid);
    }

    public Task<Void> handleNewUser() {
        // Perform all writes in one batch to ensure all complete successfully
        WriteBatch batch = db.batch();

        // Create user object
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("User must be logged in to initialize user data");
        }
        String username = user.getDisplayName() == null ? null : user.getDisplayName().trim()
                .substring(0, Math.min(user.getDisplayName().length(), 20));
        User userData = new User(username);
        UserInfo providerData = user.getProviderData().get(1);
        if (providerData.getPhotoUrl() != null) {
            userData.setProfilePicUrl(providerData.getPhotoUrl().toString());
        }
        String uid = user.getUid();
        batch.set(db.collection("users").document(uid), userData);

        // Create Pinnie wallet for new user
        final long initialBalance = 2000;
        HashMap<String, Object> wallet = new HashMap<>();
        wallet.put("currency", initialBalance);
        batch.set(db.collection("users").document(uid).collection("metadata").document("private"),
                wallet);

        // Create follower and following maps for new user
        HashMap<String, Object> followers = new HashMap<>();
        HashMap<String, Object> following = new HashMap<>();
        followers.put("followers", new ArrayList<>());
        following.put("following", new ArrayList<>());
        batch.set(db.collection("users").document(uid).collection("social").document("followers"),
                followers);
        batch.set(db.collection("users").document(uid).collection("social").document("following"),
                following);

        // Create empty activity list
        HashMap<String, Object> activity = new HashMap<>();
        activity.put("activity", new ArrayList<>());
        batch.set(db.collection("users").document(uid).collection("metadata").document("activity"),
                activity);

        return batch.commit().addOnFailureListener(e -> Log.w(TAG, "Error creating new user:", e));
    }

    public Task<OrderedPinMetadata> fetchFoundPins() {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        return db.collection("users").document(auth.getUid()).collection("pins").document("found")
                .get().continueWithTask(task -> {
                    List<Task<Pin>> fetchTasks = new ArrayList<>();
                    Map<String, Object> data = task.getResult().getData();
                    OrderedPinMetadata foundPinMetadata = new OrderedPinMetadata(data);

                    for (Iterator<PinMetadata> it = foundPinMetadata.getIterator();
                         it.hasNext(); ) {
                        String pinId = it.next().getPinId();
                        if (getCachedPin(pinId) == null) {
                            fetchTasks.add(fetchPin(pinId).addOnSuccessListener(pin -> {
                                // If pin no longer exists don't add to cache & remove from db
                                if (pin == null) {
                                    db.collection("users").document(auth.getUid())
                                            .collection("pins").document("found")
                                            .update(pinId, FieldValue.delete())
                                            .addOnSuccessListener(t -> Log.d(TAG,
                                                    "Successfully deleted found record for " +
                                                            "deleted pin."))
                                            .addOnFailureListener(e -> Log.w(TAG,
                                                    "Error deleting found record for deleted" +
                                                            " pin.",
                                                    e));
                                    foundPinMetadata.remove(pinId);
                                }
                            }));
                        }
                    }
                    return Tasks.whenAllComplete(fetchTasks).continueWith(task1 -> {
                        this.foundPinMetadata = foundPinMetadata;
                        return foundPinMetadata;
                    });
                }).addOnFailureListener(e -> Log.w(TAG, "Error fetching found pins.", e));
    }

    public OrderedPinMetadata getCachedFoundPinMetadata() {
        return foundPinMetadata;
    }

    public Task<OrderedPinMetadata> fetchDroppedPins() {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        return db.collection("users").document(auth.getUid()).collection("pins").document("dropped")
                .get().continueWithTask(task -> {
                    List<Task<Pin>> fetchTasks = new ArrayList<>();
                    Map<String, Object> data = task.getResult().getData();
                    OrderedPinMetadata droppedPinMetadata = new OrderedPinMetadata(data);

                    for (Iterator<PinMetadata> it = droppedPinMetadata.getIterator();
                         it.hasNext(); ) {
                        String pinId = it.next().getPinId();
                        if (getCachedPin(pinId) == null) {
                            fetchTasks.add(fetchPin(pinId).addOnSuccessListener(pin -> {
                                // If pin no longer exists don't add to cache & remove from db
                                if (pin == null) {
                                    db.collection("users").document(auth.getUid())
                                            .collection("pins").document("dropped")
                                            .update(pinId, FieldValue.delete())
                                            .addOnSuccessListener(t -> Log.d(TAG,
                                                    "Successfully deleted dropped record for " +
                                                            "deleted pin."))
                                            .addOnFailureListener(e -> Log.w(TAG,
                                                    "Error deleting dropped record for deleted" + " pin.",
                                                    e));
                                    droppedPinMetadata.remove(pinId);
                                }
                            }));
                        }
                    }
                    return Tasks.whenAllComplete(fetchTasks).continueWith(task1 -> {
                        this.droppedPinMetadata = droppedPinMetadata;
                        // Set current user pin metadata as well
                        this.userPinMetadata.put(auth.getUid(), droppedPinMetadata);
                        return droppedPinMetadata;
                    });
                }).addOnFailureListener(e -> Log.w(TAG, "Error fetching dropped pins.", e));
    }

    public OrderedPinMetadata getCachedDroppedPinMetadata() {
        return droppedPinMetadata;
    }

    public Task<OrderedPinMetadata> fetchUserPins(@NonNull String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        if (foundPinMetadata == null) {
            throw new IllegalStateException("Must have already fetched found pins.");
        }
        return db.collection("users").document(uid).collection("pins").document("dropped").get()
                .continueWithTask(task -> {
                    List<Task<Pin>> fetchTasks = new ArrayList<>();
                    Map<String, Object> data = task.getResult().getData();
                    OrderedPinMetadata newUserPinMetadata = new OrderedPinMetadata(data);

                    for (Iterator<PinMetadata> it = newUserPinMetadata.getIterator();
                         it.hasNext(); ) {
                        String pinId = it.next().getPinId();
                        if (getCachedPin(pinId) == null && foundPinMetadata.contains(pinId)) {
                            fetchTasks.add(fetchPin(pinId));
                        }
                    }
                    return Tasks.whenAllComplete(fetchTasks).continueWith(task1 -> {
                        userPinMetadata.put(uid, newUserPinMetadata);
                        return newUserPinMetadata;
                    });
                }).addOnFailureListener(
                        e -> Log.w(TAG, String.format("Error fetching pins for user %s.", uid), e));
    }

    public OrderedPinMetadata getCachedUserPinMetadata(@NonNull String uid) {
        return userPinMetadata.get(uid);
    }

    public Task<Pin> fetchPin(@NonNull String pid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins.");
        }
        return db.collection("pins").document(pid).get().continueWith(task -> {
            Pin pin = task.getResult().toObject(Pin.class);
            if (pin == null) {
                // Pin was deleted, delete from cache & found/dropped pins if they exist
                pins.remove(pid);
                if (foundPinMetadata != null) {
                    if (foundPinMetadata.remove(pid))
                        db.collection("users").document(auth.getUid()).collection("pins")
                                .document("found").update(pid, FieldValue.delete())
                                .addOnFailureListener(e -> Log.w(TAG,
                                        "Error deleting found record for deleted pin.", e))
                                .addOnSuccessListener(t -> Log.d(TAG,
                                        "Successfully deleted found record for deleted pin."));
                } else if (droppedPinMetadata != null) {
                    if (droppedPinMetadata.remove(pid))
                        db.collection("users").document(auth.getUid()).collection("pins")
                                .document("dropped").update(pid, FieldValue.delete())
                                .addOnFailureListener(e -> Log.w(TAG,
                                        "Error deleting dropped record for " + "deleted pin.", e))
                                .addOnSuccessListener(t -> Log.d(TAG,
                                        "Successfully deleted dropped record for deleted pin."));
                }
            } else {
                pins.put(pid, pin);
            }
            return pin;
        }).addOnFailureListener(e -> Log.w(TAG, "Error fetching pin.", e));
    }

    public Pin getCachedPin(@NonNull String pid) {
        return pins.get(pid);
    }

    public UploadTask uploadPinImage(Uri localUri, String pid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to upload pin images.");
        }
        return storage.getReference("pins").child(pid).putFile(localUri);
    }

    public void loadPinImage(ImageView imageView, Context context, String pid) {
        StorageReference ref = storage.getReference("pins").child(pid);
        GlideApp.with(context).load(ref).placeholder(R.drawable.ic_camera).centerCrop()
                .into(imageView);
    }

    public Task<String> dropPin(Pin newPin, Long cost) {
        ActivityList activity = activityMap.get(auth.getUid());
        if (activity == null) {
            throw new IllegalStateException(
                    "User must fetch their own activity before dropping a pin");
        }
        return functions.getHttpsCallable("dropPin").call(newPin.serialize()).continueWith(task -> {
            String pid = (String) task.getResult().getData();
            pins.put(pid, newPin);
            droppedPinMetadata.add(new PinMetadata(pid, newPin.getBroadLocationName(),
                    newPin.getNearbyLocationName()));
            activity.add(new ActivityItem(auth.getUid(), pid, ActivityItem.ActivityType.DROP,
                    newPin.getBroadLocationName(), newPin.getNearbyLocationName()));
            pinnies -= cost;
            return pid;
        }).addOnFailureListener(e -> Log.w(TAG, "Error dropping pin.", e));
    }

    public Task<HttpsCallableResult> findPin(String pid, Location location) {
        ActivityList activity = activityMap.get(auth.getUid());
        if (activity == null) {
            throw new IllegalStateException(
                    "User must fetch their own activity before dropping a pin");
        }
        HashMap<String, Object> data = new HashMap<>();
        data.put("pid", pid);
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());

        return functions.getHttpsCallable("findPin").call(data).addOnSuccessListener(task -> {
            // TODO: make cloud function return reward & update cached pinnie count with it
            //noinspection unchecked
            Map<String, String> result = (Map<String, String>) task.getData();
            //noinspection ConstantConditions
            String broadLocationName = result.get("broadLocationName");
            String nearbyLocationName = result.get("nearbyLocationName");
            activity.add(new ActivityItem(auth.getUid(), pid, ActivityItem.ActivityType.FIND,
                    broadLocationName, nearbyLocationName));
            foundPinMetadata.add(new PinMetadata(pid, broadLocationName, nearbyLocationName));
        }).addOnFailureListener(e -> Log.w(TAG, "Error finding pin.", e));
    }

    public Task<Map<String, Object>> fetchNearbyPins(@NonNull Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());

        //noinspection unchecked
        return functions.getHttpsCallable("getNearbyPins").call(data)
                .continueWith(task -> (Map<String, Object>) task.getResult().getData())
                .addOnFailureListener(e -> Log.w(TAG, "Error fetching nearby pins.", e));
    }

    public Task<HashSet<String>> fetchFollowers(String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch followers");
        }
        return db.collection("users").document(uid).collection("social").document("followers").get()
                .continueWith(task -> {
                    //noinspection unchecked,ConstantConditions
                    HashSet<String> followerIds =
                            new HashSet<>((List<String>) task.getResult().get("followers"));
                    userFollowerIds.put(uid, followerIds);
                    return followerIds;
                }).addOnFailureListener(
                        e -> Log.w(TAG, String.format("Error fetching followers for user %s", uid),
                                e));
    }

    public Task<HashSet<String>> fetchFollowing(String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch following");
        }
        return db.collection("users").document(uid).collection("social").document("following").get()
                .continueWith(task -> {
                    //noinspection unchecked,ConstantConditions
                    HashSet<String> followingIds =
                            new HashSet<>((List<String>) task.getResult().get("following"));
                    userFollowingIds.put(uid, followingIds);
                    return followingIds;
                }).addOnFailureListener(
                        e -> Log.w(TAG, String.format("Error fetching following for user %s", uid),
                                e));
    }

    public HashSet<String> getCachedFollowers(String uid) {
        return userFollowerIds.get(uid);
    }

    public HashSet<String> getCachedFollowing(String uid) {
        return userFollowingIds.get(uid);
    }

    public Task<Void> deletePin(String pid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to delete a pin");
        }
        WriteBatch batch = db.batch();
        batch.delete(db.collection("pins").document(pid));
        batch.update(db.collection("users").document(auth.getUid()).collection("pins")
                .document("dropped"), pid, FieldValue.delete());
        return batch.commit().addOnSuccessListener(t -> {
            Pin pin = pins.get(pid);
            if (pin != null && pin.getType() == Pin.PinType.IMAGE) {
                storage.getReference("pins").child(pid).delete()
                        .addOnFailureListener(e -> Log.w(TAG, "Error deleting pin image.", e))
                        .addOnSuccessListener(t2 -> Log.d(TAG, "Successfully deleted pin image."));
            }
            pins.remove(pid);
            droppedPinMetadata.remove(pid);
        }).addOnFailureListener(e -> Log.w(TAG, "Error deleting pin.", e));
    }

    public Task<Void> followUser(String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to follow an account");
        }
        HashSet<String> following = userFollowingIds.get(auth.getUid());
        if (following == null) {
            throw new IllegalStateException(
                    "User must have fetched their own following data before following a user");
        }
        ActivityList activity = activityMap.get(auth.getUid());
        if (activity == null) {
            throw new IllegalStateException(
                    "User must have fetched their own activity before following a user");
        }
        WriteBatch batch = db.batch();

        // Add other user to own following & increment own numFollowing
        batch.update(db.collection("users").document(auth.getUid()).collection("social")
                .document("following"), "following", FieldValue.arrayUnion(uid));
        batch.update(db.collection("users").document(auth.getUid()), "numFollowing",
                FieldValue.increment(1));

        // Add self to other user's followers & increment their numFollowers
        batch.update(
                db.collection("users").document(uid).collection("social").document("followers"),
                "followers", FieldValue.arrayUnion(auth.getUid()));
        batch.update(db.collection("users").document(uid), "numFollowers", FieldValue.increment(1));

        // Create new activity item for follow event
        ActivityItem item;
        // Don't create activity if last activity was follow of same person
        if (activity.size() == 0 || !Objects.equals(activity.get(0).getId(), uid)) {
            item = new ActivityItem(auth.getUid(), uid, ActivityItem.ActivityType.FOLLOW, null,
                    null);
            batch.update(db.collection("activity").document(auth.getUid()), "activity",
                    FieldValue.arrayUnion(item.serialize()));
        } else item = null;

        return batch.commit().addOnSuccessListener(t -> {
            following.add(uid);
            if (item != null) activity.add(item);
        }).addOnFailureListener(e -> Log.w(TAG, String.format("Error following user %s.", uid), e));
    }

    public Task<Void> unfollowUser(String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to follow an account");
        }
        HashSet<String> following = userFollowingIds.get(auth.getUid());
        if (following == null) {
            throw new IllegalStateException(
                    "User must have fetched their own following data before unfollowing a user");
        }
        WriteBatch batch = db.batch();

        // Remove other user to own following & decrement own numFollowing
        batch.update(db.collection("users").document(auth.getUid()).collection("social")
                .document("following"), "following", FieldValue.arrayRemove(uid));
        batch.update(db.collection("users").document(auth.getUid()), "numFollowing",
                FieldValue.increment(-1));

        // Remove self to other user's followers & decrement their numFollowers
        batch.update(
                db.collection("users").document(uid).collection("social").document("followers"),
                "followers", FieldValue.arrayRemove(auth.getUid()));
        batch.update(db.collection("users").document(uid), "numFollowers",
                FieldValue.increment(-1));

        return batch.commit().addOnSuccessListener(t -> following.remove(uid)).addOnFailureListener(
                e -> Log.w(TAG, String.format("Error unfollowing user %s.", uid), e));
    }

    public Task<ActivityList> fetchActivity(String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch activity");
        }
        return db.collection("users").document(uid).collection("metadata").document("activity")
                .get().continueWith(task -> {
                    ActivityList activity = task.getResult().toObject(ActivityList.class);
                    activityMap.put(uid, activity);
                    return activity;
                }).addOnFailureListener(
                        e -> Log.w(TAG, String.format("Error fetching activity for user %s.", uid),
                                e));
    }

    public ActivityList getCachedActivity(String uid) {
        return activityMap.get(uid);
    }

    public Task<Long> getPinnies() {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pinnies");
        }
        return db.collection("users").document(auth.getUid()).collection("metadata")
                .document("private").get().continueWith(task -> {
                    Long pinniesResult = (Long) task.getResult().get("currency");
                    pinnies = pinniesResult;
                    return pinniesResult;
                }).addOnFailureListener(e -> Log.w(TAG, "Error fetching pinnies.", e));
    }

    public Long getCachedPinnies() {
        return pinnies;
    }

    public Task<Integer> calcPinCost(@NonNull Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        return functions.getHttpsCallable("calcPinCost").call(data)
                .continueWith(task -> (Integer) task.getResult().getData())
                .addOnFailureListener(e -> Log.w(TAG, "Error calculating pin cost.", e));
    }
}
