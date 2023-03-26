package edu.wisc.ece.pinpoint.data;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Pin {
    private static final String TAG = Pin.class.getName();
    private String authorUID;
    private String caption;
    // contents will be a text if PinType == TEXT
    // or a URL to an image if PinType == IMAGE
    private String content;
    private PinType type;
    private Date timestamp;

    public Pin() {
    }

    public Pin(String caption, String authorUID, PinType type, String content) {
        this.caption = caption;
        this.authorUID = authorUID;
        this.type = type;
        this.content = content;
        this.timestamp = new Date();
    }

    public String getAuthorID() {
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

    public Task<DocumentReference> save() {
        Map<String, Object> pin = new HashMap<>();
        pin.put("caption", this.caption);
        pin.put("authorUID", this.authorUID);
        pin.put("content", this.content);
        pin.put("type", this.type);
        pin.put("timestamp", FieldValue.serverTimestamp());

        Log.d(TAG, "Saving the following pin to firestore: " + pin);

        return FirebaseFirestore.getInstance().collection("pins").add(pin);
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public enum PinType {
        TEXT, IMAGE
    }

}
