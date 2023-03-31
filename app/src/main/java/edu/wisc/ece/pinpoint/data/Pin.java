package edu.wisc.ece.pinpoint.data;

import android.location.Location;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
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

    public Pin() {
    }

    public Pin(String textContent, @NonNull PinType type, @NonNull Location location,
               String caption) {
        this.caption = caption;
        this.authorUID = FirebaseDriver.getInstance().getCurrentUser().getUid();
        this.type = type;
        this.textContent = textContent;
        this.timestamp = new Date();
        this.location = new GeoPoint(location.getLatitude(), location.getLongitude());
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

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        if (type == PinType.TEXT) {
            data.put("content", textContent);
        }
        data.put("type", type.toString());
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        data.put("caption", caption);
        return data;
    }

    public void loadPinPic(ImageView imageView, Fragment fragment) {
        if (type == PinType.TEXT) return;
        Glide.with(fragment).load(content).placeholder(R.drawable.ic_camera).into(imageView);
    }

    public enum PinType {
        TEXT, IMAGE
    }

}
