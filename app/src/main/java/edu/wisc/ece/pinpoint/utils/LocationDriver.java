package edu.wisc.ece.pinpoint.utils;

public class LocationDriver {
    private static LocationDriver instance;

    private LocationDriver() {
        if (instance != null) {
            throw new IllegalStateException("LocationDriver has already been instantiated.");
        }
        instance = this;
    }

    public static LocationDriver getInstance() {
        if (instance == null) {
            new LocationDriver();
        }
        return instance;
    }
}
