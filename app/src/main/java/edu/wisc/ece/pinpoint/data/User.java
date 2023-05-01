package edu.wisc.ece.pinpoint.data;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.util.Date;

import edu.wisc.ece.pinpoint.R;

public class User {
    private static final String TAG = User.class.getName();
    private String username;
    private String bio;
    private String location;
    private int numFollowers;
    private int numFollowing;
    private int numPinsDropped;
    private int numPinsFound;
    private String profilePicUrl;
    private Date profilePicTimestamp;

    public User() {
    }

    public User(String username) {
        this.username = username;
        numFollowers = 0;
        numFollowing = 0;
        numPinsDropped = 0;
        numPinsFound = 0;
    }

    public Task<Void> save(@NonNull String uid) {
        Trace trace = FirebasePerformance.getInstance().newTrace("saveUser");
        trace.start();
        return FirebaseFirestore.getInstance().collection("users").document(uid).set(this)
                .addOnCompleteListener(t -> trace.stop()).addOnSuccessListener(
                        t -> Log.d(TAG, String.format("Successfully saved user %s", uid)))
                .addOnFailureListener(e -> {
                    String message = String.format("Error saving user %s", uid);
                    Log.w(TAG, message, e);
                    FirebaseCrashlytics.getInstance().setCustomKey("message", message);
                    FirebaseCrashlytics.getInstance().recordException(e);
                });
    }

    public void loadProfilePic(ImageView imageView, Fragment fragment) {
        if (profilePicUrl != null && fragment.getActivity() != null) {
            Trace trace = FirebasePerformance.getInstance().newTrace("loadUserProfilePic");
            trace.start();
            Glide.with(fragment).load(profilePicUrl).placeholder(R.drawable.ic_profile).signature(
                            new ObjectKey(profilePicTimestamp != null ? profilePicTimestamp :
                                    "default"))
                    .circleCrop().addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e,
                                                    Object model, Target<Drawable> target,
                                                    boolean isFirstResource) {
                            trace.stop();
                            String message = "Error loading user profile picture";
                            Log.w(TAG, message, e);
                            FirebaseCrashlytics.getInstance().setCustomKey("message", message);
                            //noinspection ConstantConditions
                            FirebaseCrashlytics.getInstance().recordException(e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            trace.stop();
                            return false;
                        }
                    }).into(imageView);
        }
    }

    public String getUsername() {
        return username;
    }

    public User setUsername(@NonNull String username) {
        this.username = username;
        return this;
    }

    public String getBio() {
        return bio;
    }

    public User setBio(String bio) {
        this.bio = bio;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public User setLocation(String location) {
        this.location = location;
        return this;
    }

    public int getNumFollowers() {
        return numFollowers;
    }

    public int getNumFollowing() {
        return numFollowing;
    }

    public int getNumPinsDropped() {
        return numPinsDropped;
    }

    public int getNumPinsFound() {
        return numPinsFound;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public User setProfilePicUrl(String profilePicUrl) {
        this.profilePicTimestamp = new Date();
        this.profilePicUrl = profilePicUrl;
        return this;
    }

    public Date getProfilePicTimestamp() {
        return profilePicTimestamp;
    }

    public void setProfilePicTimestamp(Date profilePicTimestamp) {
        this.profilePicTimestamp = profilePicTimestamp;
    }
}
