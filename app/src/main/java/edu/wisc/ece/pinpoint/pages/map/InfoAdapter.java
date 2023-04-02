package edu.wisc.ece.pinpoint.pages.map;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class InfoAdapter implements GoogleMap.InfoWindowAdapter {
    private View view;
    private TextView title;
    private TextView message;
    private ImageView image;
    private LinearLayout boxAccent;
    private Pin pin;
    private Fragment fragment;

    public InfoAdapter(Context context, Fragment fragment){
        this.view = LayoutInflater.from(context)
                .inflate(R.layout.info_window_layout, null);
        this.fragment = fragment;
        title = view.findViewById(R.id.infoTitle);
        message = view.findViewById(R.id.infoMessage);
        image = view.findViewById(R.id.infoImage);
        boxAccent = view.findViewById(R.id.boxAccent);
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        setContents(marker);
        try {
            wait(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return view;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        setContents(marker);
        System.out.println("PIN DISPLAYED");
        return view;
    }

    private void setContents(@NonNull Marker marker){
        // Undiscovered pins are transparent and do not need their data fetched
        if (marker.getAlpha() != 1f){
            setUndiscoveredPinContents(marker);
        }
        else {
            System.out.println("GRABBING PIN");
            FirebaseDriver.getInstance().fetchPin(marker.getTag().toString()).addOnCompleteListener(task -> {
                pin = task.getResult();
                if (pin.getType() == Pin.PinType.IMAGE) {
                    setPicturePinContents(marker, pin);
                } else if (pin.getType() == Pin.PinType.TEXT) {
                    setTextPinContents(marker, pin);
                }
                if (title.getText().toString().isEmpty())
                    title.setVisibility(View.GONE);
                else
                    title.setVisibility(View.VISIBLE);
                System.out.println("PIN STYLING READY");
            });
        }
    }

    private void setUndiscoveredPinContents(@NonNull Marker marker){
        boxAccent.setBackgroundColor(Color.parseColor("gray"));
        title.setTextColor(Color.parseColor("gray"));
        title.setText("Undiscovered Pin");
        message.setText("Travel to this pin to reveal its contents!");
        image.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);
    }

    private void setTextPinContents(@NonNull Marker marker, Pin pin){
        boxAccent.setBackgroundColor(Color.parseColor("red"));
        title.setTextColor(Color.parseColor("red"));
        title.setText(pin.getCaption());
        message.setText(pin.getTextContent());
        image.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);
    }

    private void setPicturePinContents(@NonNull Marker marker, Pin pin){
        System.out.println("PICTURE PIN!");
        boxAccent.setBackgroundColor(Color.parseColor("green"));
        title.setTextColor(Color.parseColor("green"));
        title.setText(pin.getCaption());
        FirebaseDriver.getInstance().loadPinImage(image, fragment, marker.getTag().toString());
        image.setVisibility(View.VISIBLE);
        message.setVisibility(View.GONE);
    }
}
