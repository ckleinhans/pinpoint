package edu.wisc.ece.pinpoint.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

import javax.annotation.Nullable;

import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.User;

public final class FirebaseDriver {
    private static FirebaseDriver instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final HashMap<String, User> users;
    private final HashMap<String, Pin> pins;

    private FirebaseDriver() {
        if (instance != null) {
            throw new IllegalStateException("FirebaseDriver has already been instantiated.");
        }
        instance = this;
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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
}
