package edu.wisc.ece.pinpoint.data;

import com.google.android.gms.maps.model.LatLng;

import java.util.Map;
import java.util.Objects;

public class NearbyPinData {
    private static final String DEV_AUTHOR_UID = "pinpoint";
    private final String authorUID;
    private final LatLng location;
    private PinMetadata.PinSource source;

    public NearbyPinData(Map<String, Object> data) {
        this.authorUID = (String) data.get("authorUID");
        source = Objects.equals(authorUID, DEV_AUTHOR_UID) ? PinMetadata.PinSource.DEV :
                PinMetadata.PinSource.GENERAL;
        //noinspection ConstantConditions
        this.location = new LatLng((double) data.get("latitude"), (double) data.get("longitude"));
    }

    public String getAuthorUID() {
        return authorUID;
    }

    public LatLng getLocation() {
        return location;
    }

    public PinMetadata.PinSource getSource() {
        return source;
    }

    public void setSource(PinMetadata.PinSource source) {
        this.source = source;
    }
}
