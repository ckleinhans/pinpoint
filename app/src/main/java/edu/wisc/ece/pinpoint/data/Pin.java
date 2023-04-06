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
    private String textContent;
    private PinType type;
    private Date timestamp;
    private GeoPoint location;
    private Integer finds;
    private Long cost;

    public Pin() {}

    public Pin(String textContent, @NonNull PinType type, @NonNull Location location,
               String caption) {
        this.caption = caption;
        this.authorUID = FirebaseDriver.getInstance().getCurrentUser().getUid();
        this.type = type;
        this.textContent = textContent;
        this.timestamp = new Date();
        this.location = new GeoPoint(location.getLatitude(), location.getLongitude());
        this.finds = 0;
        this.cost = 0L;
    }

    public String getAuthorUID() {
        return authorUID;
    }

    public String getCaption() {
        return caption;
    }

    public String getTextContent() {
        return textContent;
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

    public Integer getFinds() {
        return finds == null ? 0 : finds;
    }

    public Long getCost() {
        return cost == null ? 0 : cost;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        if (type == PinType.TEXT) {
            data.put("textContent", textContent);
        }
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
