package edu.wisc.ece.pinpoint.pages.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class InfoAdapter implements GoogleMap.InfoWindowAdapter {
    private final View view;
    private final TextView title;
    private final TextView message;
    private final ImageView image;
    private final LinearLayout boxAccent;

    @SuppressLint("InflateParams")
    public InfoAdapter(Context context) {
        this.view = LayoutInflater.from(context).inflate(R.layout.view_info_window_layout, null);
        title = view.findViewById(R.id.infoTitle);
        message = view.findViewById(R.id.infoMessage);
        image = view.findViewById(R.id.infoImage);
        boxAccent = view.findViewById(R.id.boxAccent);
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        setContents(marker);
        return view;
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        setContents(marker);
        return view;
    }

    private void setContents(@NonNull Marker marker) {
        // Undiscovered pins are transparent and do not need their data fetched
        if (marker.getAlpha() != 1f) {
            setUndiscoveredPinContents();
        } else {
            //noinspection ConstantConditions
            Pin pin = FirebaseDriver.getInstance().getCachedPin(marker.getTag().toString());
            if (pin.getType() == Pin.PinType.IMAGE) {
                setPicturePinContents(marker, pin);
            } else if (pin.getType() == Pin.PinType.TEXT) {
                setTextPinContents(pin);
            }
            if (title.getText().toString().isEmpty()) title.setVisibility(View.GONE);
            else title.setVisibility(View.VISIBLE);
        }
        String color = "#CC0000";
        switch (marker.getSnippet()){
            case "SELF": color = "#0080FF"; break;
            case "DEV": color = "#CCCC00"; break;
            case "NFC": color = "#00CCCC"; break;
            case "FRIEND": color = "#00CC00"; break;
        }
        boxAccent.setBackgroundColor(Color.parseColor(color));
        title.setTextColor(Color.parseColor(color));
    }

    private void setUndiscoveredPinContents() {
        title.setText(R.string.undiscovered_pin_title);
        message.setText(R.string.undiscovered_pin_message);
        image.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);
    }

    private void setTextPinContents(Pin pin) {
        title.setText(pin.getCaption());
        message.setText(pin.getTextContent());
        image.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);
    }

    private void setPicturePinContents(@NonNull Marker marker, Pin pin) {
        title.setText(pin.getCaption());
        //noinspection ConstantConditions
        FirebaseDriver.getInstance()
                .loadPinImage(image, view.getContext(), marker.getTag().toString());
        image.setVisibility(View.VISIBLE);
        message.setVisibility(View.GONE);
    }
}
