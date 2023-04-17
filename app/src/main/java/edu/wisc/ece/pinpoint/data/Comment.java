package edu.wisc.ece.pinpoint.data;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class Comment {

    private String content;
    private String authorUID;
    @ServerTimestamp Date timestamp;

    public Comment() {}

    public Comment(String content) {
        FirebaseDriver instance = FirebaseDriver.getInstance();
        this.content = content;
        this.authorUID = instance
                .getCurrentUser()
                .getUid();
    }

    public Date getTimestamp() { return timestamp; }
    public String getAuthorUID() { return authorUID; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public void setAuthorUID(String authorUID) { this.authorUID = authorUID; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public HashMap<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("content", this.content);
        data.put("authorUID", this.authorUID);
        data.put("timestamp", this.timestamp);
        return data;
    }
}
