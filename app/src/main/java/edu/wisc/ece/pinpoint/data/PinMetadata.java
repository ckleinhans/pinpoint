package edu.wisc.ece.pinpoint.data;

import java.util.Date;

public class PinMetadata {
    private final String pinId;
    // Time at which a user dropped/found a pin
    private final Date timestamp;

    public PinMetadata(String pinId, Date timestamp) {
        this.pinId = pinId;
        this.timestamp = timestamp;
    }

    public String getPinId() {
        return pinId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        return pinId.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PinMetadata) {
            return this.pinId.equals(((PinMetadata) other).pinId);
        } else {
            return false;
        }
    }
}
