package edu.wisc.ece.pinpoint.pages.pins;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.PinListRecyclerData;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinListAdapter extends RecyclerView.Adapter<PinListAdapter.PinListViewHolder> {
    private final List<PinListRecyclerData> pinList;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private Context parentContext;

    public PinListAdapter(List<PinListRecyclerData> pinList, NavController navController) {
        this.pinList = pinList;
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
        PinListRecyclerData pinItem = pinList.get(position);
        String pid = pinItem.getId();

        if (pinItem.getPin().getType() == Pin.PinType.IMAGE) {
            firebase.loadPinImage(holder.image, parentContext, pid);
        }

        holder.item.setOnClickListener(view -> navController.navigate(
                edu.wisc.ece.pinpoint.NavigationDirections.pinView(pid)));
    }

    @Override
    public int getItemCount() {
        return pinList.size();
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