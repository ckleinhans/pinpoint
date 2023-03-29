package edu.wisc.ece.pinpoint.data;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class Pin {
    private static final String TAG = Pin.class.getName();
    private String authorUID;
    private String caption;
    // contents will be a text if PinType == TEXT
    // or a URL to an image if PinType == IMAGE
    private String content;
    private PinType type;
    private Date timestamp;
    private GeoPoint location;

    public Pin() {
    }

    public Pin(String caption, String authorUID, PinType type, String content, GeoPoint location) {
        this.caption = caption;
        this.authorUID = authorUID;
        this.type = type;
        this.content = content;
        this.timestamp = new Date();
        this.location = location;
    }

    public String getAuthorUID() {
        return authorUID;
    }

    public String getCaption() {
        return caption;
    }

    public String getContent() {
        return content;
    }

    public PinType getType() {
        return type;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public enum PinType {
        TEXT, IMAGE
    }

}
