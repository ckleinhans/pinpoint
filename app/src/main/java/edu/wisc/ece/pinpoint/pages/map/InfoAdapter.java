package edu.wisc.ece.pinpoint.pages.map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import edu.wisc.ece.pinpoint.R;

public class InfoAdapter implements GoogleMap.InfoWindowAdapter {

    private View view;

    public InfoAdapter(Context context){
        this.view = LayoutInflater.from(context)
                .inflate(R.layout.info_window_layout, null);
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        setInfoTitle(marker);
        return view;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        setInfoTitle(marker);
        return view;
    }

    private void setInfoTitle(@NonNull Marker marker){
        // TODO: SET CONTENTS ACCORDING TO TAG -> PIN ID
        TextView tv = view.findViewById(R.id.infoTitle);
        tv.setText(marker.getTitle());
    }
}
