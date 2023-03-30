package edu.wisc.ece.pinpoint.pages.pins;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import edu.wisc.ece.pinpoint.R;

public class PinListAdapter extends RecyclerView.Adapter<PinListAdapter.PinListViewHolder> {
    private final ArrayList<RecyclerData> pinList;
    NavController navController;

    public PinListAdapter(ArrayList<RecyclerData> pinList, NavController navController) {
        this.pinList = pinList;
        this.navController = navController;
    }

    @NonNull
    @Override
    public PinListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_pin_list_item, parent, false);
        return new PinListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PinListViewHolder holder, int position) {
        // Set the data to textview and imageview.
        RecyclerData recyclerData = pinList.get(position);
        holder.pinImage.setImageResource(recyclerData.getImgid());
    }

    @Override
    public int getItemCount() {
        return pinList.size();
    }

    public void openPinView() {
        // Hardcoded pin ID for testing purposes
        navController.navigate(PinListFragmentDirections.pinView("57uAc4DnqyBNdN6hn3NP"));
    }

    // View Holder Class to handle Recycler View.
    public class PinListViewHolder extends RecyclerView.ViewHolder {
        private final ImageView pinImage;

        public PinListViewHolder(@NonNull View itemView) {
            super(itemView);

            pinImage = itemView.findViewById(R.id.pinlist_item_image);

            itemView.findViewById(R.id.pinlist_item).setOnClickListener(view -> openPinView());
        }

    }
}