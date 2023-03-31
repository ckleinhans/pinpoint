package edu.wisc.ece.pinpoint.pages.pins;

import edu.wisc.ece.pinpoint.data.Pin;

public class RecyclerData {

    private String pinId;
    private Pin pin;

    public RecyclerData(String pinId, Pin pin) {
        this.pinId = pinId;
        this.pin = pin;
    }

    public String getId() {

        return pinId;
    }

    public Pin getPin(){
        return pin;
    }

}
