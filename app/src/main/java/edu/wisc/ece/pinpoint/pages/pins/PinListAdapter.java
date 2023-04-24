package edu.wisc.ece.pinpoint.pages.pins;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.chip.Chip;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.NearbyPinData;
import edu.wisc.ece.pinpoint.data.OrderedPinMetadata;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.PinMetadata;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;
import edu.wisc.ece.pinpoint.utils.LocationDriver;

public class PinListAdapter extends RecyclerView.Adapter<PinListAdapter.PinListViewHolder> {
    private final OrderedPinMetadata pinMetadata;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private final PinListFragment.PinListType listType;
    private Context parentContext;
    private LocationDriver locationDriver;

    // TODO: make this adapter a ListAdapter to improve UX & performance
    public PinListAdapter(OrderedPinMetadata pinMetadata, NavController navController,
                          PinListFragment.PinListType listType) {
        this.pinMetadata = pinMetadata;
        this.navController = navController;
        this.firebase = FirebaseDriver.getInstance();
        this.listType = listType;
    }

    @NonNull
    @Override
    public PinListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_pin_list_item, parent, false);
        parentContext = parent.getContext();
        this.locationDriver = LocationDriver.getInstance(parentContext);
        return new PinListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PinListViewHolder holder, int position) {
        PinMetadata metadata = pinMetadata.get(position);
        String pid = metadata.getPinId();

        // Set pin type chip
        holder.pinSourceChip.setVisibility(View.GONE);
        holder.pinSourceChip.setCloseIcon(null);
        if (metadata.getPinSource() == PinMetadata.PinSource.SELF) { // && listType ==
            // PinListFragment.PinListType.DROPPED
            holder.pinSourceChip.setChipBackgroundColorResource(R.color.my_pins);
            holder.pinSourceChip.setText(FormatUtils.trimmedNumber(metadata.getCost()));
            holder.pinSourceChip.setCloseIcon(
                    ResourcesCompat.getDrawable(parentContext.getResources(),
                            R.drawable.ic_pinnies_logo, parentContext.getTheme()));
            holder.pinSourceChip.setVisibility(View.VISIBLE);
        } else if (metadata.getPinSource() == PinMetadata.PinSource.NFC) {
            holder.pinSourceChip.setChipBackgroundColorResource(R.color.nfc_pins);
            holder.pinSourceChip.setText(R.string.nfc_text);
            holder.pinSourceChip.setVisibility(View.VISIBLE);
        } else if (metadata.getPinSource() == PinMetadata.PinSource.DEV) {
            holder.pinSourceChip.setChipBackgroundColorResource(R.color.landmark_pins);
            holder.pinSourceChip.setText(R.string.landmark_text);
            holder.pinSourceChip.setVisibility(View.VISIBLE);
        } else if (metadata.getPinSource() == PinMetadata.PinSource.GENERAL && listType != PinListFragment.PinListType.USER) {
            // don't show other or following tags on user profile pages
            if (firebase.getCachedFollowing(firebase.getUid())
                    .contains(firebase.getCachedPin(metadata.getPinId()).getAuthorUID())) {
                holder.pinSourceChip.setChipBackgroundColorResource(R.color.friend_pins);
                holder.pinSourceChip.setText(R.string.following_text);
            } else {
                holder.pinSourceChip.setChipBackgroundColorResource(R.color.other_pins);
                holder.pinSourceChip.setText(R.string.other_text);
            }
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
            holder.item.setOnClickListener(view -> {
                NearbyPinData pinData = firebase.getCachedNearbyPin(pid);
                if (pinData == null) {
                    Toast.makeText(parentContext, R.string.undiscovered_pin_locked,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                locationDriver.getLastLocation(parentContext)
                        .addOnCompleteListener(locationTask -> {
                            Location userLoc = locationTask.getResult();
                            if (!locationTask.isSuccessful() || userLoc == null) {
                                Toast.makeText(parentContext, R.string.location_error_text,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (LocationDriver.isCloseEnoughToFindPin(
                                    new LatLng(userLoc.getLatitude(), userLoc.getLongitude()),
                                    pinData.getLocation())) {
                                firebase.findPin(pid, userLoc, pinData.getSource())
                                        .addOnSuccessListener(reward -> {
                                            Toast.makeText(parentContext, String.format(
                                                    parentContext.getString(
                                                            R.string.pinnie_reward_message),
                                                    reward), Toast.LENGTH_LONG).show();
                                            navController.navigate(
                                                    edu.wisc.ece.pinpoint.pages.map.MapContainerFragmentDirections.pinView(
                                                            pid));
                                        }).addOnFailureListener(
                                                e -> Toast.makeText(parentContext, e.getMessage(),
                                                        Toast.LENGTH_LONG).show());
                            } else {
                                Toast.makeText(parentContext,
                                        R.string.undiscovered_pin_not_close_enough,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            });
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