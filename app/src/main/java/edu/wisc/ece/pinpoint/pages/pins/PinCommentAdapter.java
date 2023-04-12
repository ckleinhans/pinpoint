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

public class PinCommentAdapter extends RecyclerView.Adapter<PinCommentAdapter.PinCommentViewHolder> {

    private final FirebaseDriver firebase;

    private final NavController navController;

    private Context parentContext;

    public PinCommentAdapter(NavController navController) {
        firebase = FirebaseDriver.getInstance();
        this.navController = navController;
    }

    @NonNull
    @Override
    public PinCommentAdapter.PinCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_pin_list_item, parent, false);
        parentContext = parent.getContext();
        return new PinCommentAdapter.PinCommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PinCommentAdapter.PinCommentViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        //TODO: Add function to get number of comments on a pin
        return 1;
    }

    public static class PinCommentViewHolder extends RecyclerView.ViewHolder {
        private final CardView item;
        private final ImageView image;
        private final TextView overlayText;

        public PinCommentViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.pinlist_item);
            image = itemView.findViewById(R.id.pinlist_item_image);
            overlayText = itemView.findViewById(R.id.pinlist_image_text);
        }
    }
}
