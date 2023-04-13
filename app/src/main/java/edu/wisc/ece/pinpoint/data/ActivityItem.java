package edu.wisc.ece.pinpoint.data;

import java.util.Date;

public class ActivityItem {
    private String author;
    private String id;
    private ActivityType type;
    private Date timestamp;

    public ActivityItem() {
    }

    public ActivityItem(String author, String id, ActivityType type, Date timestamp) {
        this.author = author;
        this.id = id;
        this.type = type;
        this.timestamp = new Date();
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

    public enum ActivityType {
        DROP, FIND, COMMENT, FOLLOW
    }
}