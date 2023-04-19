package edu.wisc.ece.pinpoint.data;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.Map;

public class PinMetadata {

    // Time at which a user dropped/found a pin
    private final Date timestamp;
    private final String broadLocationName;
    private final String nearbyLocationName;
    private final Long reward;
    private final Long cost;
    private final String pinId;

    public PinMetadata(String pinId, Map<String, Object> data) {
        this.pinId = pinId;
        //noinspection ConstantConditions
        this.timestamp = ((Timestamp) data.get("timestamp")).toDate();
        this.broadLocationName = (String) data.get("broadLocationName");
        this.nearbyLocationName = (String) data.get("nearbyLocationName");
        this.reward = (Long) data.get("reward");
        this.cost = (Long) data.get("cost");
    }

    public PinMetadata(String pinId, String broadLocationName, String nearbyLocationName) {
        this.pinId = pinId;
        this.timestamp = new Date();
        this.broadLocationName = broadLocationName;
        this.nearbyLocationName = nearbyLocationName;
        this.reward = null;
        this.cost = null;
    }

    public String getPinId() {
        return pinId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getBroadLocationName() {
        return broadLocationName;
    }

    public String getNearbyLocationName() {
        return nearbyLocationName;
    }

    public long getReward() {
        return reward;
    }

    public long getCost() {
        return cost;
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
