package edu.wisc.ece.pinpoint.utils;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.GlideApp;
import edu.wisc.ece.pinpoint.data.OrderedHashSet;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.User;

public class FirebaseDriver {
    private static FirebaseDriver instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseFunctions functions;
    private final Map<String, User> users;
    private final Map<String, Pin> pins;
    private OrderedHashSet<String> foundPinIds;
    private OrderedHashSet<String> droppedPinIds;

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
        if (onComplete != null) {
            user.sendEmailVerification().addOnCompleteListener(onComplete);
        } else {
            user.sendEmailVerification();
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

    // TODO: improve fetch efficiency using query
    // TODO: order results by timestamp
    public Task<OrderedHashSet<String>> fetchFoundPins() {
        OrderedHashSet<String> foundPinIds = new OrderedHashSet<>();
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        return db.collection("users").document(auth.getUid()).collection("found").get()
                .continueWithTask(task -> {
                    List<Task<Pin>> fetchTasks = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                        String pinId = documentSnapshot.getId();
                        foundPinIds.add(pinId);
                        if (getCachedPin(pinId) == null) {
                            fetchTasks.add(fetchPin(pinId));
                        }
                    }
                    return Tasks.whenAllComplete(fetchTasks).continueWith(task1 -> {
                        this.foundPinIds = foundPinIds;
                        return foundPinIds;
                    });
                });
    }

    public OrderedHashSet<String> getCachedFoundPinIds() {
        return foundPinIds;
    }

    // TODO: improve fetch efficiency using query
    // TODO: order results by timestamp
    public Task<OrderedHashSet<String>> fetchDroppedPins() {
        OrderedHashSet<String> droppedPinIds = new OrderedHashSet<>();
        if (auth.getUid() == null) {
            throw new IllegalStateException("User must be logged in to fetch pins");
        }
        return db.collection("users").document(auth.getUid()).collection("dropped").get()
                .continueWithTask(task -> {
                    List<Task<Pin>> fetchTasks = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                        String pinId = documentSnapshot.getId();
                        droppedPinIds.add(pinId);
                        if (getCachedPin(pinId) == null) {
                            fetchTasks.add(fetchPin(pinId));
                        }
                    }
                    return Tasks.whenAllComplete(fetchTasks).continueWith(task1 -> {
                        this.droppedPinIds = droppedPinIds;
                        return droppedPinIds;
                    });
                });
    }

    public OrderedHashSet<String> getCachedDroppedPinIds() {
        return droppedPinIds;
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

    public Task<String> dropPin(Pin newPin) {
        return functions.getHttpsCallable("dropPin").call(newPin.serialize()).continueWith(task -> {
            String pid = (String) task.getResult().getData();
            pins.put(pid, newPin);
            droppedPinIds.add(pid);
            return pid;
        });
    }
    public Task<Long> getPinnies(String uid) {
        return db
                .collection("private")
                .document(uid)
                .get()
                .continueWith(task -> (Long) task.getResult().get("currency"));
    }

    public Task<Long> calcPinCost(@NonNull Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        return functions.getHttpsCallable("calcPinCost").call(data)
                .continueWith(task -> (Long) task.getResult().getData());
    }

    public Task<Map<String, Object>> fetchNearbyPins(@NonNull Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        
        //noinspection unchecked
        return functions.getHttpsCallable("getNearbyPins").call(data)
                .continueWith(task -> (Map<String, Object>) task.getResult().getData());
    }
}
