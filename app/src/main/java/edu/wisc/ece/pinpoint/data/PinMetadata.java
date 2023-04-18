package edu.wisc.ece.pinpoint.data;

import java.util.Date;

public class PinMetadata {

    private String pinId;
    // Time at which a user dropped/found a pin
    private Date timestamp;
    private String broadLocationName;
    private String nearbyLocationName;
    private int reward;
    private int cost;
    private PinSource pinSource;

    public PinMetadata() {
    }

    public PinMetadata(String pinId, String broadLocationName, String nearbyLocationName, PinSource pinSource) {
        this.pinId = pinId;
        this.timestamp = new Date();
        this.broadLocationName = broadLocationName;
        this.nearbyLocationName = nearbyLocationName;
        this.reward = 0;
        this.cost = 0;
        this.pinSource = pinSource;
    }

    public String getPinId() {
        return pinId;
    }

    public void setPinId(String pinId) {
        this.pinId = pinId;
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

    public int getReward() {
        return reward;
    }

    public int getCost() {
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

    public PinSource getPinSource() {return pinSource;}

    public enum PinSource {
        SELF, GENERAL, NFC, DEV
    }
}
