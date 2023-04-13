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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
    private OrderedPinMetadata foundPinMetadata;
    private OrderedPinMetadata droppedPinMetadata;
    private Long pinnies;
    private HashSet<String> followerIds;
    private HashSet<String> followingIds;
    private final HashMap<String, ActivityList> activityMap;

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
        followerIds = null;
        followingIds = null;
        return AuthUI.getInstance().signOut(context);
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
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
        });
    }

    public User getCachedUser(@NonNull String uid) {
        return users.get(uid);
    }

    public void handleNewUser() {
        // User is new, push user data to new node in DB
        FirebaseUser user = getCurrentUser();
        User userData = new User(user.getDisplayName());
        UserInfo providerData = user.getProviderData().get(1);
        if (providerData.getPhotoUrl() != null) {
            userData.setProfilePicUrl(providerData.getPhotoUrl().toString());
        }
        String uid = user.getUid();
        userData.save(uid);

        // create Pinnie wallet for new user
        final long initialBalance = 2000;
        HashMap<String, Object> data = new HashMap<>();
        data.put("currency", initialBalance);
        db.collection("private").document(uid).set(data).addOnSuccessListener(
                        t -> Log.d(TAG, String.format("Wallet for user %s created!", uid)))
                .addOnFailureListener(e -> Log.w(TAG, "Error creating wallet document", e));

        // create follower and following sets for new user
        HashMap<String, Object> socials = new HashMap<>();
        socials.put("followers", null);
        socials.put("following", null);
        db.collection("social").document(uid).set(socials);
    }

    public Task<OrderedPinMetadata> fetchFoundPins() {
        OrderedPinMetadata foundPinMetadata = new OrderedPinMetadata();
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        return db.collection("users").document(auth.getUid()).collection("found")
                .orderBy("timestamp").get().continueWithTask(task -> {
                    List<Task<Pin>> fetchTasks = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                        String pinId = documentSnapshot.getId();
                        foundPinMetadata.add(new PinMetadata(pinId,
                                documentSnapshot.get("timestamp", Date.class)));
                        if (getCachedPin(pinId) == null) {
                            fetchTasks.add(fetchPin(pinId));
                        }
                    }
                    return Tasks.whenAllComplete(fetchTasks).continueWith(task1 -> {
                        this.foundPinMetadata = foundPinMetadata;
                        return foundPinMetadata;
                    });
                });
    }

    public OrderedPinMetadata getCachedFoundPinMetadata() {
        return foundPinMetadata;
    }

    public Task<OrderedPinMetadata> fetchDroppedPins() {
        OrderedPinMetadata droppedPinMetadata = new OrderedPinMetadata();
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        return db.collection("users").document(auth.getUid()).collection("dropped")
                .orderBy("timestamp").get().continueWithTask(task -> {
                    List<Task<Pin>> fetchTasks = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                        String pinId = documentSnapshot.getId();
                        droppedPinMetadata.add(new PinMetadata(pinId,
                                documentSnapshot.get("timestamp", Date.class)));
                        if (getCachedPin(pinId) == null) {
                            fetchTasks.add(fetchPin(pinId));
                        }
                    }
                    return Tasks.whenAllComplete(fetchTasks).continueWith(task1 -> {
                        this.droppedPinMetadata = droppedPinMetadata;
                        // Set current user pin metadata as well
                        this.userPinMetadata.put(auth.getUid(), droppedPinMetadata);
                        return droppedPinMetadata;
                    });
                });
    }

    public OrderedPinMetadata getCachedDroppedPinMetadata() {
        return droppedPinMetadata;
    }

    public Task<OrderedPinMetadata> fetchUserPins(@NonNull String uid) {
        OrderedPinMetadata newUserPinMetadata = new OrderedPinMetadata();
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        if (foundPinMetadata == null) {
            throw new IllegalStateException("Must have already fetched found pins.");
        }
        return db.collection("users").document(uid).collection("dropped").orderBy("timestamp").get()
                .continueWithTask(task -> {
                    List<Task<Pin>> fetchTasks = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                        String pinId = documentSnapshot.getId();
                        newUserPinMetadata.add(new PinMetadata(pinId,
                                documentSnapshot.get("timestamp", Date.class)));
                        // Only fetch pins that the user has found
                        if (getCachedPin(pinId) == null && foundPinMetadata.contains(pinId)) {
                            fetchTasks.add(fetchPin(pinId));
                        }
                    }
                    return Tasks.whenAllComplete(fetchTasks).continueWith(task1 -> {
                        userPinMetadata.put(uid, newUserPinMetadata);
                        return newUserPinMetadata;
                    });
                });
    }

    public OrderedPinMetadata getCachedUserPinMetadata(@NonNull String uid) {
        return userPinMetadata.get(uid);
    }

    public Task<Pin> fetchPin(@NonNull String pid) {
        return db.collection("pins").document(pid).get().continueWith(task -> {
            Pin pin = task.getResult().toObject(Pin.class);
            pins.put(pid, pin);
            return pin;
        });
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
        return functions.getHttpsCallable("dropPin").call(newPin.serialize()).continueWith(task -> {
            String pid = (String) task.getResult().getData();
            pins.put(pid, newPin);
            droppedPinMetadata.add(new PinMetadata(pid, new Date()));
            pinnies -= cost;
            return pid;
        });
    }

    public Task<Long> getPinnies() {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pinnies");
        }
        return db.collection("private").document(auth.getUid()).get().continueWith(task -> {
            Long pinniesResult = (Long) task.getResult().get("currency");
            pinnies = pinniesResult;
            return pinniesResult;
        });
    }

    public Long getCachedPinnies() {
        return pinnies;
    }

    public Task<Integer> calcPinCost(@NonNull Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        return functions.getHttpsCallable("calcPinCost").call(data)
                .continueWith(task -> (Integer) task.getResult().getData());
    }

    public Task<Pin> findPin(String pid, Location location) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("pid", pid);
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());

        return functions.getHttpsCallable("findPin").call(data).continueWith(task -> {
            // For now don't do anything with returned pin data since it will be fetched
            // on pin view page load
            foundPinMetadata.add(new PinMetadata(pid, new Date()));
            return null;
        });
    }

    public Task<Map<String, Object>> fetchNearbyPins(@NonNull Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());

        //noinspection unchecked
        return functions.getHttpsCallable("getNearbyPins").call(data)
                .continueWith(task -> (Map<String, Object>) task.getResult().getData());
    }

    // TODO: improve fetch efficiency using query
    public void fetchSocials() {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch followers");
        }
        followerIds = new HashSet<>();
        followingIds = new HashSet<>();
        db.collection("social").document(auth.getUid()).get().continueWith(task -> {
            DocumentSnapshot doc = task.getResult();
            ArrayList<String> followers = doc.get("followers") != null ? (ArrayList) doc.get("followers") :
                    new ArrayList<>();
            ArrayList<String> following = doc.get("following") != null ? (ArrayList) doc.get("following") :
                    new ArrayList<>();
            for(String person : followers){
                followerIds.add(person);
            }
            for(String person : following){
                followingIds.add(person);
            }
            db.collection("users").document(auth.getUid()).update("numFollowers",
                    followerIds.size());
            db.collection("users").document(auth.getUid()).update("numFollowing",
                    followingIds.size());
            return null;
        });
    }

    public HashSet<String> getCachedFollowerIds() {return followerIds;}

    public HashSet<String> getCachedFollowingIds() {return followingIds;}

    public void followUser(String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to follow an account");
        }
        Date timestamp = new Date();
        followingIds.add(uid);
        // Add target to following
        db.collection("social")
                .document(auth.getUid()).update("following", FieldValue.arrayUnion(uid))
                .addOnSuccessListener(complete -> db.collection("users").document(
                        auth.getUid()).update("numFollowing", FieldValue.increment(1)));


        // Add self to target's followers
        db.collection("social")
                .document(uid).update("followers", FieldValue.arrayUnion(auth.getUid()))
                .addOnSuccessListener(complete -> db.collection("users").document(uid)
                        .update("numFollowers", FieldValue.increment(1)));

        pushActivityItem(new ActivityItem(auth.getUid(), uid, ActivityItem.ActivityType.FOLLOW, timestamp));
    }

    public void unfollowUser(String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to follow an account");
        }
        followingIds.remove(uid);
        // Remove target from following list
        db.collection("social")
                .document(auth.getUid()).update("following", FieldValue.arrayRemove(uid))
                .addOnSuccessListener(complete -> db.collection("users").document(
                        auth.getUid()).update("numFollowing", FieldValue.increment(-1)));

        // Remove self from target's followers
        db.collection("social")
                .document(uid).update("followers", FieldValue.arrayRemove(auth.getUid()))
                .addOnSuccessListener(complete -> db.collection("users").document(uid)
                        .update("numFollowers", FieldValue.increment(-1)));
    }

    public Task<ActivityList> fetchActivity(String uid) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch activity");
        }
        return db.collection("activity").document(uid).get().continueWith(task -> {
            ActivityList activity;
            if (!task.getResult().exists())
                activity = new ActivityList();
            else activity = task.getResult().toObject(ActivityList.class);
            activityMap.put(uid, activity);
            return activity;
        });
    }

    public ActivityList getCachedActivity(String uid) {
        if(activityMap.containsKey(uid))
            return activityMap.get(uid);
        return null;
    }

    public void pushActivityItem(ActivityItem activityItem) {
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to push activity");
        }
        String authorId = activityItem.getAuthor();
        if (!activityMap.containsKey(authorId))
            activityMap.put(authorId, new ActivityList());
        activityMap.get(authorId).add(activityItem);
        // Create map to store field name and activity item
        HashMap<String, Object> map = new HashMap<>();
        map.put("activity",FieldValue.arrayUnion(activityItem));
        // Push activity item to firebase
        db.collection("activity").document(authorId).set(map, SetOptions.merge());
    }
}
