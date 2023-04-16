package edu.wisc.ece.pinpoint.pages.pins;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.OrderedPinMetadata;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.PinMetadata;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;

public class PinListAdapter extends RecyclerView.Adapter<PinListAdapter.PinListViewHolder> {
    private final OrderedPinMetadata pinMetadata;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private Context parentContext;

    public PinListAdapter(OrderedPinMetadata pinMetadata, NavController navController) {
        this.pinMetadata = pinMetadata;
        this.navController = navController;
        firebase = FirebaseDriver.getInstance();
    }

    @NonNull
    @Override
    public PinListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_pin_list_item, parent, false);
        parentContext = parent.getContext();
        return new PinListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PinListViewHolder holder, int position) {
        PinMetadata metadata = pinMetadata.get(position);
        String pid = metadata.getPinId();

        if (firebase.getCachedFoundPinMetadata()
                .contains(pid) || firebase.getCachedDroppedPinMetadata().contains(pid)) {
            // Pin is discovered
            Pin pin = firebase.getCachedPin(pid);
            if (pin.getType() == Pin.PinType.IMAGE) {
                firebase.loadPinImage(holder.image, parentContext, pid);
            } else {
                holder.image.setImageResource(R.drawable.oldnote);
            }
            holder.item.setOnClickListener(view -> navController.navigate(
                    edu.wisc.ece.pinpoint.NavigationDirections.pinView(pid)));
        } else {
            // Pin is undiscovered
            holder.image.setImageResource(R.drawable.ic_lock);
            holder.item.setOnClickListener(
                    view -> Toast.makeText(parentContext, R.string.undiscovered_pin_locked,
                            Toast.LENGTH_SHORT).show());
        }

        if (metadata.getBroadLocationName() != null) {
            holder.broadLocation.setText(metadata.getBroadLocationName());
        } else {
            holder.broadLocation.setVisibility(View.GONE);
        }
        if (metadata.getNearbyLocationName() != null) {
            holder.nearbyLocation.setText(metadata.getNearbyLocationName());
        } else {
            holder.nearbyLocation.setVisibility(View.GONE);
        }
        holder.timestamp.setText(FormatUtils.formattedDateTime(metadata.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return pinMetadata.size();
    }

    // View Holder Class to handle Recycler View.
    public static class PinListViewHolder extends RecyclerView.ViewHolder {
        private final CardView item;
        private final ImageView image;
        private final TextView timestamp;
        private final TextView nearbyLocation;
        private final TextView broadLocation;

        public PinListViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.pinlist_item);
            image = itemView.findViewById(R.id.pinlist_item_image);
            timestamp = itemView.findViewById(R.id.pinlist_image_timestamp);
            nearbyLocation = itemView.findViewById(R.id.pinlist_image_nearby_location);
            broadLocation = itemView.findViewById(R.id.pinlist_image_broad_location);
        }
    }
}