package edu.wisc.ece.pinpoint.data;

import android.util.Log;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import edu.wisc.ece.pinpoint.R;

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

    public Task<DocumentReference> save() {
        Map<String, Object> pin = new HashMap<>();
        pin.put("caption", this.caption);
        pin.put("authorUID", this.authorUID);
        pin.put("content", this.content);
        pin.put("type", this.type);
        pin.put("timestamp", FieldValue.serverTimestamp());
        pin.put("location", location);

        Log.d(TAG, "Saving the following pin to firestore: " + pin);

        return FirebaseFirestore.getInstance().collection("pins").add(pin);
    }

    public void loadPinPic(ImageView imageView, Fragment fragment) {
        if (type == PinType.TEXT) return;
        // TODO: choose a placeholder for the pin pic
        Glide.with(fragment).load(content).placeholder(R.drawable.ic_profile).into(imageView);
    }

    public enum PinType {
        TEXT, IMAGE
    }

}
