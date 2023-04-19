package edu.wisc.ece.pinpoint.data;

import com.google.firebase.firestore.FieldValue;

import java.util.Date;
import java.util.HashMap;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class Comment {

    private String content;
    private String authorUID;
    private Date timestamp;

    public Comment() {
    }

    public Comment(String content) {
        FirebaseDriver instance = FirebaseDriver.getInstance();
        this.content = content;
        this.authorUID = instance.getUid();
        this.timestamp = new Date();
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getAuthorUID() {
        return authorUID;
    }

    public String getContent() {
        return content;
    }

    public HashMap<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("content", this.content);
        data.put("authorUID", this.authorUID);
        data.put("timestamp", FieldValue.serverTimestamp());
        return data;
    }
}
