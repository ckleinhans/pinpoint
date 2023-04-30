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
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
import edu.wisc.ece.pinpoint.data.Comment;
import edu.wisc.ece.pinpoint.data.GlideApp;
import edu.wisc.ece.pinpoint.data.NearbyPinData;
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
    private final FirebaseCrashlytics crashlytics;
    private final Map<String, User> users;
    private final Map<String, Pin> pins;
    private final Map<String, OrderedPinMetadata> userPinMetadata;
    private final HashMap<String, ActivityList> activityMap;
    private final HashMap<String, HashSet<String>> userFollowerIds;
    private final HashMap<String, HashSet<String>> userFollowingIds;
    private final HashMap<String, NearbyPinData> nearbyPins;
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
        crashlytics = FirebaseCrashlytics.getInstance();
        users = new HashMap<>();
        pins = new HashMap<>();
        userPinMetadata = new HashMap<>();
        activityMap = new HashMap<>();
        userFollowerIds = new HashMap<>();
        userFollowingIds = new HashMap<>();
        nearbyPins = new HashMap<>();
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
        Task<Void> sendEmailTask = user.sendEmailVerification().addOnFailureListener(
                        e -> handleError(e,
                                String.format("Error sending email verification to " + "user %s",
                                        auth.getUid())))
                .addOnSuccessListener(t -> Log.d(TAG, "Successfully sent verification email"));
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
        return AuthUI.getInstance().signOut(context).addOnSuccessListener(t -> {
                    foundPinMetadata = null;
                    droppedPinMetadata = null;
                    pinnies = null;
                    crashlytics.setUserId("");
                }).addOnFailureListener(
                        e -> handleError(e, String.format("Error logging out user %s",
                                auth.getUid())))
                .addOnSuccessListener(t -> Log.d(TAG, "Successfully logged out"));
    }

    public Task<Void> deleteAccount(@NonNull Context context) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to check if they are new.");
        }
        String uid = auth.getUid();
        return functions.getHttpsCallable("deleteAccount").call().continueWithTask(t -> {
                    if (!t.isSuccessful()) //noinspection ConstantConditions
                        throw t.getException();
                    users.remove(uid);
                    return logout(context);
                }).addOnFailureListener(
                        e -> handleError(e, String.format("Error deleting account %s", uid)))
                .addOnSuccessListener(
                        t -> Log.d(TAG, String.format("Successfully deleted user %s", uid)));
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
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch users");
        }
        return db.collection("users").document(uid).get().continueWith(task -> {
                    User user = task.getResult().toObject(User.class);
                    if (user == null) {
                        // user deleted, remove from followers/following
                        if (getCachedFollowing(auth.getUid()).remove(uid)) {
                            // Remove other user from own following & decrement own numFollowing
                            WriteBatch batch = db.batch();
                            batch.update(db.collection("users").document(auth.getUid()).collection("social")
                                    .document("following"), "following",
                                    FieldValue.arrayRemove(uid));
                            batch.update(db.collection("users").document(auth.getUid()),
                                    "numFollowing",
                                    FieldValue.increment(-1));
                            batch.commit().addOnFailureListener(e -> handleError(e,
                                    String.format("Error removing deleted user %s from " +
                                                    "following",
                                            uid))).addOnSuccessListener(t -> Log.d(TAG,
                                    String.format(
                                    "Successfully removed deleted user %s from " + "following",
                                            uid)));
                        }
                        if (getCachedFollowers(auth.getUid()).remove(uid)) {
                            // Remove other user from own followers & decrement own numFollowers
                            WriteBatch batch = db.batch();
                            batch.update(db.collection("users").document(auth.getUid()).collection("social")
                                    .document("followers"), "followers",
                                    FieldValue.arrayRemove(uid));
                            batch.update(db.collection("users").document(auth.getUid()),
                                    "numFollowers",
                                    FieldValue.increment(-1));
                            batch.commit().addOnFailureListener(e -> handleError(e,
                                    String.format("Error removing deleted user %s from " +
                                                    "followers",
                                            uid))).addOnSuccessListener(t -> Log.d(TAG,
                                    String.format(
                                    "Successfully removed deleted user %s from " + "followers",
                                            uid)));
                        }
                    }
                    users.put(uid, user);
                    return user;
                }).addOnFailureListener(
                        e -> handleError(e, String.format("Error fetching user " + "%s", uid)))
                .addOnSuccessListener(
                        user -> Log.d(TAG, String.format("Successfully fetched user %s", uid)));
    }

    public User getCachedUser(@NonNull String uid) {
        return users.get(uid);
    }

    public boolean isUserCached(@NonNull String uid) {
        return users.containsKey(uid);
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

        return batch.commit().addOnFailureListener(
                        e -> handleError(e, String.format("Error creating new user %s",
                                user.getUid())))
                .addOnSuccessListener(t -> Log.d(TAG,
                        String.format("Successfully created new user %s", user.getUid())));
    }

    public Task<OrderedPinMetadata> fetchFoundPins() {
        OrderedPinMetadata foundPinMetadata = new OrderedPinMetadata();
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        CollectionReference foundRef =
                db.collection("users").document(auth.getUid()).collection("found");
        return foundRef.orderBy("timestamp").get().continueWithTask(task -> {
                    List<Task<Pin>> fetchTasks = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                        String pinId = documentSnapshot.getId();
                        //noinspection ConstantConditions
                        PinMetadata metadata = new PinMetadata(pinId, documentSnapshot.getData());
                        foundPinMetadata.add(metadata);
                        if (getCachedPin(pinId) == null) {
                            fetchTasks.add(fetchPin(pinId).addOnSuccessListener(pin -> {
                                // If pin no longer exists don't add to cache & remove from db
                                if (pin == null) {
                                    db.collection("users").document(auth.getUid()).collection(
                                            "found")
                                            .document(pinId).delete().addOnFailureListener(
                                                    e -> handleError(e, String.format(
                                                            "Error deleting found record for " +
                                                                    "deleted pin " + "%s",
                                                            pinId))).addOnSuccessListener(t2 -> Log.d(TAG,
                                                    String.format(
                                                            "Successfully deleted found record " + "for " + "deleted " + "pin %s",
                                                            pinId)));
                                    foundPinMetadata.remove(pinId);
                                }
                            }));
                        }
                    }
                    return Tasks.whenAllComplete(fetchTasks).continueWith(task1 -> {
                        this.foundPinMetadata = foundPinMetadata;
                        return foundPinMetadata;
                    });
                }).addOnFailureListener(e -> handleError(e, "Error fetching found pins"))
                .addOnSuccessListener(pins -> Log.d(TAG,
                        String.format("Successfully fetched %d found pins", pins.size())));
    }

    public OrderedPinMetadata getCachedFoundPinMetadata() {
        return foundPinMetadata;
    }

    public Task<OrderedPinMetadata> fetchDroppedPins() {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        return db.collection("users").document(auth.getUid()).collection("dropped")
                .document("dropped").get().continueWithTask(task -> {
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
                                            .collection("dropped").document("dropped")
                                            .update(pinId, FieldValue.delete())
                                            .addOnSuccessListener(t -> Log.d(TAG, String.format(
                                                    "Successfully deleted dropped record for " +
                                                            "deleted pin %s",
                                                    pinId))).addOnFailureListener(
                                                    e -> handleError(e, String.format(
                                                            "Error deleting dropped record for " + "deleted pin %s",
                                                            pinId)));
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
                }).addOnFailureListener(e -> handleError(e, "Error fetching dropped pins"))
                .addOnSuccessListener(pins -> Log.d(TAG,
                        String.format("Successfully fetched %d dropped pins", pins.size())));
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
        return db.collection("users").document(uid).collection("dropped").document("dropped").get()
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
                        e -> handleError(e, String.format("Error fetching pins for user %s.", uid)))
                .addOnSuccessListener(pins -> Log.d(TAG,
                        String.format("Successfully fetched user %s pins, length %d", uid,
                                pins.size())));
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
                                db.collection("users").document(auth.getUid()).collection("found")
                                        .document(pid).delete().addOnFailureListener(e -> handleError(e,
                                                String.format("Error deleting found record for " + "deleted pin %s",
                                                        pid))).addOnSuccessListener(t -> Log.d(TAG, String.format(
                                                "Successfully deleted found record for deleted " + "pin %s", pid)));
                        } else if (droppedPinMetadata != null) {
                            if (droppedPinMetadata.remove(pid))
                                db.collection("users").document(auth.getUid()).collection("pins")
                                        .document("dropped").update(pid, FieldValue.delete())
                                        .addOnFailureListener(e -> handleError(e, String.format(
                                                "Error deleting dropped record for deleted pin " + "%s",
                                                pid))).addOnSuccessListener(t -> Log.d(TAG,
                                                String.format(
                                                "Successfully deleted dropped record for deleted "
                                                        + "pin %s",
                                                pid)));
                        }
                    } else {
                        pins.put(pid, pin);
                    }
                    return pin;
                }).addOnFailureListener(e -> handleError(e, String.format("Error fetching pin %s"
                        , pid)))
                .addOnSuccessListener(
                        pin -> Log.d(TAG, String.format("Successfully fetched pin %s", pid)));
    }

    public Pin getCachedPin(@NonNull String pid) {
        return pins.get(pid);
    }

    public UploadTask uploadPinImage(Uri localUri, String pid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to upload pin images.");
        }
        UploadTask task = storage.getReference("pins").child(pid).putFile(localUri);
        task.addOnFailureListener(
                        e -> handleError(e, String.format("Error uploading photo for pin %s", pid)))
                .addOnSuccessListener(t -> Log.d(TAG,
                        String.format("Successfully uploaded photo for pin %s", pid)));
        return task;
    }

    // TODO: add on resource ready optional parameter to reload the info window for map view
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
                    newPin.getNearbyLocationName(), PinMetadata.PinSource.SELF, cost));
            activity.add(new ActivityItem(auth.getUid(), pid, ActivityItem.ActivityType.DROP,
                    newPin.getBroadLocationName(), newPin.getNearbyLocationName()));
            pinnies -= cost;
            return pid;
        }).addOnFailureListener(e -> handleError(e, "Error dropping pin")).addOnSuccessListener(
                pid -> Log.d(TAG, String.format("Successfully dropped pin %s", pid)));
    }

    public Task<Integer> findPin(String pid, Location location, PinMetadata.PinSource pinSource) {
        ActivityList activity = activityMap.get(auth.getUid());
        if (activity == null) {
            throw new IllegalStateException(
                    "User must fetch their own activity before dropping a pin");
        }
        HashMap<String, Object> data = new HashMap<>();
        data.put("pid", pid);
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        data.put("pinSource", pinSource.name());

        return functions.getHttpsCallable("findPin").call(data).continueWith(task -> {
                    //noinspection unchecked
                    Map<String, Object> result = (Map<String, Object>) task.getResult().getData();

                    // remove pin from nearby pins
                    nearbyPins.remove(pid);

                    //noinspection ConstantConditions
                    String broadLocationName = (String) result.get("broadLocationName");
                    String nearbyLocationName = (String) result.get("nearbyLocationName");
                    //noinspection ConstantConditions
                    int reward = (int) result.get("reward");
                    pinnies += reward;

                    activity.add(new ActivityItem(auth.getUid(), pid,
                            ActivityItem.ActivityType.FIND,
                            broadLocationName, nearbyLocationName));
                    foundPinMetadata.add(
                            new PinMetadata(pid, broadLocationName, nearbyLocationName, pinSource
                                    , null));

                    return reward;
                }).addOnFailureListener(e -> handleError(e, String.format("Error finding pin %s",
                        pid)))
                .addOnSuccessListener(reward -> Log.d(TAG,
                        String.format("Successfully found pin %s for reward %d", pid, reward)));
    }

    public Task<HashMap<String, NearbyPinData>> fetchNearbyPins(@NonNull Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());

        return functions.getHttpsCallable("getNearbyPins").call(data).continueWith(task -> {
                    nearbyPins.clear();
                    //noinspection unchecked
                    Map<String, Map<String, Object>> res =
                            (Map<String, Map<String, Object>>) task.getResult().getData();
                    //noinspection ConstantConditions
                    res.forEach((pid, pinData) -> nearbyPins.put(pid, new NearbyPinData(pinData)));
                    return nearbyPins;
                }).addOnFailureListener(e -> handleError(e, "Error fetching nearby pins."))
                .addOnSuccessListener(list -> Log.d(TAG,
                        String.format("Successfully fetched %d nearby pins", list.size())));
    }

    public NearbyPinData getCachedNearbyPin(String pid) {
        return nearbyPins.get(pid);
    }

    public Task<String> reportPin(String pid) {
        Map<String, Object> data = new HashMap<>();
        data.put("pid", pid);
        return functions.getHttpsCallable("reportPin").call(data)
                .continueWith(task -> (String) task.getResult().getData()).addOnSuccessListener(
                        t -> Log.d(TAG, String.format("Successfully reported pin %s", pid)))
                .addOnFailureListener(
                        e -> handleError(e, String.format("Error reporting pin %s", pid)));
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
                    Log.d(TAG,
                            String.format("Successfully fetched user %s followers, length %d", uid,
                                    followerIds.size()));
                    return followerIds;
                }).addOnFailureListener(e -> handleError(e,
                        String.format("Error fetching followers for user %s", uid)));
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
                    Log.d(TAG,
                            String.format("Successfully fetched user %s following, length %d", uid,
                                    followingIds.size()));
                    return followingIds;
                }).addOnFailureListener(e -> handleError(e,
                        String.format("Error fetching following for user %s", uid)));
    }

    public HashSet<String> getCachedFollowers(String uid) {
        return userFollowerIds.get(uid);
    }

    public HashSet<String> getCachedFollowing(String uid) {
        return userFollowingIds.get(uid);
    }

    public Task<HttpsCallableResult> deletePin(String pid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to delete a pin");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("pid", pid);
        return functions.getHttpsCallable("deletePin").call(data).addOnSuccessListener(t -> {
            pins.remove(pid);
            droppedPinMetadata.remove(pid);
            Log.d(TAG, String.format("Successfully deleted pin %s", pid));
        }).addOnFailureListener(e -> handleError(e, String.format("Error deleting pin %s", pid)));
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
            batch.update(db.collection("users").document(auth.getUid()).collection("metadata")
                    .document("activity"), "activity", FieldValue.arrayUnion(item.serialize()));
        } else item = null;

        return batch.commit().addOnSuccessListener(t -> {
            following.add(uid);
            Log.d(TAG, String.format("Successfully followed user %s", uid));
            if (item != null) activity.add(item);
        }).addOnFailureListener(
                e -> handleError(e, String.format("Error following user %s.", uid)));
    }

    public Task<Void> unfollowUser(String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to follow an account");
        }
        HashSet<String> following = userFollowingIds.get(auth.getUid());
        if (following == null) {
            throw new IllegalStateException(
                    "User must have fetched their own following data before unfollowing a " +
                            "user");
        }
        WriteBatch batch = db.batch();

        // Remove other user from own following & decrement own numFollowing
        batch.update(db.collection("users").document(auth.getUid()).collection("social")
                .document("following"), "following", FieldValue.arrayRemove(uid));
        batch.update(db.collection("users").document(auth.getUid()), "numFollowing",
                FieldValue.increment(-1));

        // Remove self from other user's followers & decrement their numFollowers
        batch.update(
                db.collection("users").document(uid).collection("social").document("followers"),
                "followers", FieldValue.arrayRemove(auth.getUid()));
        batch.update(db.collection("users").document(uid), "numFollowers",
                FieldValue.increment(-1));

        return batch.commit().addOnSuccessListener(t -> {
            following.remove(uid);
            Log.d(TAG, String.format("Successfully unfollowed user %s", uid));
        }).addOnFailureListener(
                e -> handleError(e, String.format("Error unfollowing user %s.", uid)));
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
                }).addOnFailureListener(e -> handleError(e,
                        String.format("Error fetching activity for user %s.", uid)))
                .addOnSuccessListener(list -> Log.d(TAG,
                        String.format("Successfully fetched user %s activity, length %d", uid,
                                list.size())));
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
                }).addOnFailureListener(e -> handleError(e, "Error fetching pinnies"))
                .addOnSuccessListener(pinnies -> Log.d(TAG,
                        String.format("Successfully fetched user pinnies: %d", pinnies)));
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
                .addOnFailureListener(e -> handleError(e, "Error calculating pin cost"))
                .addOnSuccessListener(cost -> Log.d(TAG,
                        String.format("Successfully calculated pin cost of %d", cost)));
    }

    public Task<Void> postComment(Comment comment, String pid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to post a comment");
        }
        Pin pin = getCachedPin(pid);
        if (pin == null) {
            throw new IllegalStateException("User must have fetched pin before posting a comment");
        }
        ActivityList activity = activityMap.get(auth.getUid());
        if (activity == null) {
            throw new IllegalStateException(
                    "User must have fetched their own activity before posting a comment");
        }
        WriteBatch batch = db.batch();

        // Create new comment document
        batch.set(db.collection("pins").document(pid).collection("comments").document(),
                comment.serialize());

        // Create new activity item for comment event
        ActivityItem item;
        // Don't create activity if last activity was comment of same pin
        if (activity.size() == 0 || !Objects.equals(activity.get(0).getId(), pid) || activity.get(0)
                .getType() != ActivityItem.ActivityType.COMMENT) {
            item = new ActivityItem(auth.getUid(), pid, ActivityItem.ActivityType.COMMENT,
                    pin.getBroadLocationName(), pin.getNearbyLocationName());
            batch.update(db.collection("users").document(auth.getUid()).collection("metadata")
                    .document("activity"), "activity", FieldValue.arrayUnion(item.serialize()));
        } else item = null;

        return batch.commit().addOnSuccessListener(t -> {
            Log.d(TAG, String.format("Successfully added comment to pin %s", pid));
            if (item != null) activity.add(item);
        }).addOnFailureListener(
                e -> handleError(e, String.format("Error commenting on pin %s", pid)));
    }

    public Task<List<Comment>> fetchComments(String pid) {
        return db.collection("pins").document(pid).collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING).get().continueWith(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, String.format("Successfully fetched %d comments for pin %s",
                                task.getResult().size(), pid));
                        return task.getResult().toObjects(Comment.class);
                    } else {
                        handleError(task.getException(),
                                String.format("Error fetching comments for pin %s", pid));
                        return null;
                    }
                });
    }

    public void handleError(Throwable e, String message) {
        Log.w(TAG, message, e);
        crashlytics.setCustomKey("message", message);
        crashlytics.recordException(e);
    }
}
