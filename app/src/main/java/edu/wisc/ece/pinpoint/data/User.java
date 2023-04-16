package edu.wisc.ece.pinpoint.data;

import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

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

    public User(String username) {
        this.username = username;
        numFollowers = 0;
        numFollowing = 0;
        numPinsDropped = 0;
        numPinsFound = 0;
    }

    public Task<Void> save(@NonNull String uid) {
        return FirebaseFirestore.getInstance().collection("users").document(uid).set(this);
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
