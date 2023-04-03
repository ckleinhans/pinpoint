package edu.wisc.ece.pinpoint.data;

import androidx.annotation.NonNull;

public class PinListRecyclerData {
    private final String pinId;
    private final Pin pin;

    public PinListRecyclerData(@NonNull String pinId, @NonNull Pin pin) {
        this.pinId = pinId;
        this.pin = pin;
    }

    public String getId() {
        return pinId;
    }

    public Pin getPin() {
        return pin;
    }
}
