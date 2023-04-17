package edu.wisc.ece.pinpoint.data;

import java.util.Date;

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

    public enum ActivityType {
        DROP, FIND, COMMENT, FOLLOW
    }
}
