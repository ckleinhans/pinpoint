package edu.wisc.ece.pinpoint.data;

import java.util.Date;
import java.util.HashMap;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class Comment {
    private final String content;
    private final String authorUID;
    private final Date timestamp;

    public Comment(String content) {
        this.content = content;
        this.authorUID = FirebaseDriver.getInstance()
                .getCurrentUser()
                .getUid();
        this.timestamp = new Date();
    }

    public Date getTimestamp() { return timestamp; }
    public String getAuthorUID() { return authorUID; }
    public String getContent() { return content; }

    public HashMap<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("content", this.content);
        data.put("authorUID", this.authorUID);
        return data;
    }
}
