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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import edu.wisc.ece.pinpoint.R;

public class InfoAdapter implements GoogleMap.InfoWindowAdapter {
    private View view;
    TextView title;
    TextView message;
    ImageView image;
    LinearLayout boxAccent;


    public InfoAdapter(Context context){
        this.view = LayoutInflater.from(context)
                .inflate(R.layout.info_window_layout, null);
        title = view.findViewById(R.id.infoTitle);
        message = view.findViewById(R.id.infoMessage);
        image = view.findViewById(R.id.infoImage);
        boxAccent = view.findViewById(R.id.boxAccent);
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        setContents(marker);
        return view;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        setContents(marker);
        return view;
    }

    private void setContents(@NonNull Marker marker){
        // TODO: SET CONTENTS ACCORDING TO TAG -> PIN ID
        setPicturePinContents(marker);
        if (title.getText().toString().isEmpty())
            title.setVisibility(View.GONE);

    }

    private void setUndiscoveredPinContents(@NonNull Marker marker){
        boxAccent.setBackgroundColor(Color.parseColor("gray"));
        title.setTextColor(Color.parseColor("gray"));
        title.setText("Undiscovered Pin");
        message.setText("Travel to this pin to reveal its contents!");
        image.setVisibility(View.GONE);

    }

    private void setTextPinContents(@NonNull Marker marker){
        boxAccent.setBackgroundColor(Color.parseColor("red"));
        title.setTextColor(Color.parseColor("red"));
        title.setText("Text Pin");
        message.setText("This is the message of a text pin.");
        image.setVisibility(View.GONE);
    }

    private void setPicturePinContents(@NonNull Marker marker){
        boxAccent.setBackgroundColor(Color.parseColor("green"));
        title.setTextColor(Color.parseColor("green"));
        title.setText("Picture Pin");
    }
}
