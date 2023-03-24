package edu.wisc.ece.pinpoint.data;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Pin {
    public enum PinType {
        TEXT,
        IMAGE
    }
    private static final String TAG = Pin.class.getName();
    private String authorUID;
    private String caption;
    // contents will be a text if PinType == TEXT
    // or a URL to an image if PinType == IMAGE
    private String contents;
    private PinType type;

    public Pin(String caption, String authorUID, PinType type, String contents) {
        this.caption = caption;
        this.authorUID = authorUID;
        this.type = type;
        this.contents = contents;
    }

    // bruh I hate Java
    public String getAuthorID() { return authorUID; }
    public String getCaption() { return caption; }
    public String getContents() { return contents; }
    public PinType getType() { return type; }

    public Task<DocumentReference> save() {
        // handcrafting a map here instead of using reflection in Java
        // since I was getting weird auth errors otherwise.
        Map<String, Object> pin = new HashMap<>();
        pin.put("caption", this.caption);
        pin.put("authorUID", this.authorUID);
        pin.put("contents", this.contents);
        pin.put("type", String.valueOf(this.type));
        pin.put("timestamp", FieldValue.serverTimestamp());

        Log.d(TAG, "Saving the following pin to firestore: " + pin.toString());

        return FirebaseFirestore.getInstance()
            .collection("pins")
            .add(pin);
    }

}
