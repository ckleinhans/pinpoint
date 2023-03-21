package edu.wisc.ece.pinpoint.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class User {
    private String username;
    private String bio;
    private String location;
    private int numFollowers;
    private int numFollowing;
    private int numPinsDropped;
    private int numPinsFound;

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

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public void setNumPinsDropped(int numPinsDropped) {
        this.numPinsDropped = numPinsDropped;
    }

    public int getNumPinsFound() {
        return numPinsFound;
    }

    public void setNumPinsFound(int numPinsFound) {
        this.numPinsFound = numPinsFound;
    }
}
