package edu.wisc.ece.pinpoint.data;

import android.location.Location;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

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

    public Pin(@NonNull String content, @NonNull PinType type, @NonNull Location location,
               String caption) {
        this.caption = caption;
        this.authorUID = FirebaseDriver.getInstance().getCurrentUser().getUid();
        this.type = type;
        this.content = content;
        this.timestamp = new Date();
        this.location = new GeoPoint(location.getLatitude(), location.getLongitude());
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

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("type", type.toString());
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        data.put("caption", caption);
        return data;
    }

    public enum PinType {
        TEXT, IMAGE
    }

}
