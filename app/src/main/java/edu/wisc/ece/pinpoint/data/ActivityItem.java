package edu.wisc.ece.pinpoint.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ActivityItem {
    private String author;
    private String id;
    private ActivityType type;
    private Date timestamp;
    private String broadLocationName;
    private String nearbyLocationName;

    public ActivityItem() {
    }

    public ActivityItem(String author, String id, ActivityType type, String broadLocationName,
                        String nearbyLocationName) {
        this.author = author;
        this.id = id;
        this.type = type;
        this.timestamp = new Date();
        this.broadLocationName = broadLocationName;
        this.nearbyLocationName = nearbyLocationName;
    }

    public String getAuthor() {
        return author;
    }

    public String getId() {
        return id;
    }

    public ActivityType getType() {
        return type;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getNearbyLocationName() {
        return nearbyLocationName;
    }

    public String getBroadLocationName() {
        return broadLocationName;
    }

    public Map<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("author", author);
        data.put("id", id);
        data.put("type", type);
        data.put("timestamp", timestamp);
        data.put("broadLocationName", broadLocationName);
        data.put("nearbyLocationName", nearbyLocationName);
        return data;
    }

    public enum ActivityType {
        DROP, FIND, COMMENT, FOLLOW
    }
}
