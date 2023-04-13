package edu.wisc.ece.pinpoint.data;

import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import edu.wisc.ece.pinpoint.R;

public class User {
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

    public User(String username, String uid) {
        this.username = username;
        numFollowers = 0;
        numFollowing = 0;
        numPinsDropped = 0;
        numPinsFound = 0;
        // Save new user to Firestore
        FirebaseFirestore.getInstance().collection("users").document(uid).set(this);
    }

    public User(String username, String uid, String profilePicUrl) {
        this.username = username;
        numFollowers = 0;
        numFollowing = 0;
        numPinsDropped = 0;
        numPinsFound = 0;
        this.profilePicTimestamp = new Date();
        this.profilePicUrl = profilePicUrl;
        // Save new user to Firestore
        FirebaseFirestore.getInstance().collection("users").document(uid).set(this);
    }

    public Task<Void> save(@NonNull String uid) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("bio", bio);
        data.put("location", location);
        data.put("profilePicUrl", profilePicUrl);
        data.put("profilePicTimestamp", profilePicTimestamp);
        return FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(data, SetOptions.merge());
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

    public void setNumFollowers(int numFollowers) {
        this.numFollowers = numFollowers;
    }

    public int getNumFollowing() {
        return numFollowing;
    }

    public void setNumFollowing(int numFollowing) {
        this.numFollowing = numFollowing;
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
        return this.setProfilePicUrl(profilePicUrl, true);
    }

    public User setProfilePicUrl(String profilePicUrl, boolean updateTimestamp) {
        if (updateTimestamp) this.profilePicTimestamp = new Date();
        this.profilePicUrl = profilePicUrl;
        return this;
    }

    public Date getProfilePicTimestamp() {
        return profilePicTimestamp;
    }

    public void loadProfilePic(ImageView imageView, Fragment fragment) {
        if (profilePicUrl != null) {
            Glide.with(fragment).load(profilePicUrl).placeholder(R.drawable.ic_profile).signature(
                            new ObjectKey(profilePicTimestamp != null ? profilePicTimestamp :
                                    "default"))
                    .circleCrop().into(imageView);
        }
    }
}
