package edu.wisc.ece.pinpoint.pages.pins;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.OrderedHashSet;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinListAdapter extends RecyclerView.Adapter<PinListAdapter.PinListViewHolder> {
    private final OrderedHashSet<String> pinIds;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private Context parentContext;

    public PinListAdapter(OrderedHashSet<String> pinIds, NavController navController) {
        this.pinIds = pinIds;
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
        String pid = pinIds.get(position);
        if (firebase.getCachedFoundPinIds().contains(pid) || firebase.getCachedDroppedPinIds()
                .contains(pid)) {
            if (firebase.getCachedPin(pid).getType() == Pin.PinType.IMAGE) {
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
    }

    @Override
    public int getItemCount() {
        return pinIds.size();
    }

    // View Holder Class to handle Recycler View.
    public static class PinListViewHolder extends RecyclerView.ViewHolder {
        private final CardView item;
        private final ImageView image;

        public PinListViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.pinlist_item);
            image = itemView.findViewById(R.id.pinlist_item_image);
        }
    }
}