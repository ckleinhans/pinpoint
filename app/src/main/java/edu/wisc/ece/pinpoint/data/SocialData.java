package edu.wisc.ece.pinpoint.data;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashSet;
import java.util.List;

public class SocialData {
    private final HashSet<String> followers;
    private final HashSet<String> following;

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public SocialData(DocumentSnapshot doc) {
        followers = new HashSet<>();
        following = new HashSet<>();
        followers.addAll((List<String>) doc.get("followers"));
        following.addAll((List<String>) doc.get("following"));
    }

    public HashSet<String> getFollowers() {
        return followers;
    }

    public HashSet<String> getFollowing() {
        return following;
    }

    public void addFollowing(String uid) {
        following.add(uid);
    }

    public void removeFollowing(String uid) {
        following.remove(uid);
    }
}
