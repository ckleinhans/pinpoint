package edu.wisc.ece.pinpoint.pages.pins;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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
        holder.item.setOnClickListener(view ->  navController.navigate(edu.wisc.ece.pinpoint.NavigationDirections.pinView(recyclerData.getId())));

    }

    @Override
    public int getItemCount() {
        return pinList.size();
    }



    // View Holder Class to handle Recycler View.
    public class PinListViewHolder extends RecyclerView.ViewHolder {

        private final CardView item;

        public PinListViewHolder(@NonNull View itemView) {
            super(itemView);

            item = itemView.findViewById(R.id.pinlist_item);
        }

    }
}