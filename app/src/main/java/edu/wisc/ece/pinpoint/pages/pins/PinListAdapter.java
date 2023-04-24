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

import com.google.android.material.chip.Chip;

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
    private final boolean displayPinTypes;
    private Context parentContext;

    // TODO: make this adapter a ListAdapter to improve UX & performance
    public PinListAdapter(OrderedPinMetadata pinMetadata, NavController navController,
                          boolean displayPinTypes) {
        this.pinMetadata = pinMetadata;
        this.navController = navController;
        firebase = FirebaseDriver.getInstance();
        this.displayPinTypes = displayPinTypes;
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

        // Set pin type chip
        if (displayPinTypes) {
            switch (metadata.getPinSource()) {
                case SELF:
                    holder.pinSourceChip.setChipBackgroundColorResource(R.color.my_pins);
                    holder.pinSourceChip.setText(R.string.my_pins_text);
                    break;
                case NFC:
                    holder.pinSourceChip.setChipBackgroundColorResource(R.color.nfc_pins);
                    holder.pinSourceChip.setText(R.string.nfc_text);
                    break;
                case DEV:
                    holder.pinSourceChip.setChipBackgroundColorResource(R.color.landmark_pins);
                    holder.pinSourceChip.setText(R.string.landmarks_text);
                    break;
                case GENERAL:
                    // check for friend pin
                    if (firebase.getCachedFollowing(firebase.getUid())
                            .contains(firebase.getCachedPin(metadata.getPinId()).getAuthorUID())) {
                        holder.pinSourceChip.setChipBackgroundColorResource(R.color.friend_pins);
                        holder.pinSourceChip.setText(R.string.following_text);
                    } else {
                        holder.pinSourceChip.setChipBackgroundColorResource(R.color.other_pins);
                        holder.pinSourceChip.setText(R.string.other_text);
                    }
                    break;
            }
            holder.pinSourceChip.setVisibility(View.VISIBLE);
        } else {
            holder.pinSourceChip.setVisibility(View.GONE);
        }

        // show discovered or undiscovered pin
        if (firebase.getCachedFoundPinMetadata()
                .contains(pid) || firebase.getCachedDroppedPinMetadata().contains(pid)) {
            // Pin is discovered
            Pin pin = firebase.getCachedPin(pid);
            if (pin.getType() == Pin.PinType.IMAGE) {
                firebase.loadPinImage(holder.image, parentContext, pid);
            } else {
                holder.image.setImageResource(R.drawable.pin_background_img);
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

        // Set location text
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

        // Set timestamp
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
        private final Chip pinSourceChip;

        public PinListViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.pinlist_item);
            image = itemView.findViewById(R.id.pinlist_item_image);
            timestamp = itemView.findViewById(R.id.pinlist_image_timestamp);
            nearbyLocation = itemView.findViewById(R.id.pinlist_image_nearby_location);
            broadLocation = itemView.findViewById(R.id.pinlist_image_broad_location);
            pinSourceChip = itemView.findViewById(R.id.pinlist_pin_source);
        }
    }
}