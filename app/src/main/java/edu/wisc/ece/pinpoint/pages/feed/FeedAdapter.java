package edu.wisc.ece.pinpoint.pages.feed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

import edu.wisc.ece.pinpoint.NavigationDirections;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.ActivityItem;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
    private final List<ActivityItem> activity;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private Context parentContext;
    private final Fragment fragment;

    public FeedAdapter(List<ActivityItem> activity, NavController navController, Fragment fragment) {
        this.activity = activity;
        this.navController = navController;
        this.fragment = fragment;
        firebase = FirebaseDriver.getInstance();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.feed_single_item, parent, false);
        parentContext = parent.getContext();
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        // Single action in activity list
        ActivityItem action = activity.get(position);
        String id = action.getId()
        String author = action.getAuthor();
        String type = action.getType();
        Date time = action.getTimestamp();
        // Set timestamp
        holder.timestamp.setText(FormatUtils.formattedDateTime(time));
        // Clicking on the picture of the action's author will navigate to their profile
        holder.image.setOnClickListener(view ->
                navController.navigate(NavigationDirections.profile().setUid(author)));
        // Clicking on the card will navigate to the action's relevant page
        holder.item.setOnClickListener(view -> {
            if (type.equals("drop") || type.equals("find") || type.equals("comment")) {
                if (firebase.getCachedFoundPinMetadata()
                        .contains(id) || firebase.getCachedDroppedPinMetadata().contains(id))
                    navController.navigate(NavigationDirections.pinView(id));
                else
                    Toast.makeText(parentContext, R.string.undiscovered_pin_locked,
                                    Toast.LENGTH_SHORT).show();

            } else if (type.equals("follow")) {
                navController.navigate(NavigationDirections.profile().setUid(id));
            }
        });
        User cachedAuthor = firebase.getCachedUser(author);
        if (cachedAuthor != null) {
            // put author data in feed item
            setContents(cachedAuthor, type, id, holder);
        } else {
            // Since only using author for profile pic & username, only fetch if not cached
            firebase.fetchUser(author)
                    .addOnCompleteListener(task -> setContents(task.getResult(), type, id, holder));
        }
    }

    private void setContents(User author, String type, String id, @NonNull FeedViewHolder holder){
        author.loadProfilePic(holder.image, fragment);
        String username = author.getUsername();
        String textContents = "";
        switch(type){
            case "drop":
                textContents = username + " dropped a pin";
                break;
            case "find":
                textContents = username + " found a pin";
                break;
            case "comment":
                textContents = username + " commented on a pin";
                break;
            case "follow":
                User cachedPinAuthor = firebase.getCachedUser(id);
                if (cachedPinAuthor != null) {
                    textContents = username + " followed " + cachedPinAuthor.getUsername();
                } else {
                    firebase.fetchUser(id).addOnCompleteListener(task -> {
                        String finalTextContents =
                                username + " followed " + task.getResult().getUsername();
                        holder.text.setText(finalTextContents);
                    });
                }
                break;
        }
        if (!textContents.isEmpty())
            holder.text.setText(textContents);

    }

    @Override
    public int getItemCount() {
        return activity.size();
    }

    // View Holder Class to handle Recycler View.
    public static class FeedViewHolder extends RecyclerView.ViewHolder {
        private final CardView item;
        private final ImageView image;
        private final TextView text;
        private final TextView timestamp;


        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.feed_item);
            image = itemView.findViewById(R.id.feed_item_image);
            text = itemView.findViewById(R.id.feed_item_text);
            timestamp = itemView.findViewById(R.id.feed_item_time);
        }
    }
}