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

import edu.wisc.ece.pinpoint.NavigationDirections;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.ActivityItem;
import edu.wisc.ece.pinpoint.data.ActivityList;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
    private final ActivityList activity;
    private final FeedSource source;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private final Fragment fragment;
    private Context parentContext;

    public FeedAdapter(ActivityList activity, NavController navController, Fragment fragment,
                       FeedSource source) {
        this.activity = activity;
        this.source = source;
        this.navController = navController;
        this.fragment = fragment;
        firebase = FirebaseDriver.getInstance();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_feed_single_item, parent, false);
        parentContext = parent.getContext();
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        ActivityItem action = activity.get(position);
        String author = action.getAuthor();

        User cachedAuthor = firebase.getCachedUser(author);
        if (cachedAuthor != null) {
            setContents(cachedAuthor, action, holder);
        } else {
            // Since only using author for profile pic & username, only fetch if not cached
            firebase.fetchUser(author)
                    .addOnCompleteListener(task -> setContents(task.getResult(), action, holder));
        }
    }

    @Override
    public int getItemCount() {
        return activity.size();
    }

    private void setContents(User author, ActivityItem action, @NonNull FeedViewHolder holder) {
        String id = action.getId();
        ActivityItem.ActivityType type = action.getType();

        holder.timestamp.setText(FormatUtils.formattedDateTime(action.getTimestamp()));

        author.loadProfilePic(holder.image, fragment);

        // Detect if the activity belongs to current user, and trim if username is too long
        String username = action.getAuthor().equals(firebase.getCurrentUser().getUid()) ? "You" :
                author.getUsername();
        String location;
        String textContents = "";
        switch (type) {
            case DROP:
                location = FormatUtils.formattedPinLocation(action.getBroadLocationName(),
                        action.getNearbyLocationName());
                textContents = location == null ? username + " dropped a pin." :
                        username + " dropped a pin near " + location;
                holder.icon.setImageResource(R.drawable.ic_drop);
                break;
            case FIND:
                location = FormatUtils.formattedPinLocation(action.getBroadLocationName(),
                        action.getNearbyLocationName());
                textContents = location == null ? username + " found a pin." :
                        username + " found a pin near " + location;
                holder.icon.setImageResource(R.drawable.ic_search);
                break;
            case COMMENT:
                location = FormatUtils.formattedPinLocation(action.getBroadLocationName(),
                        action.getNearbyLocationName());
                textContents = location == null ? username + " commented on a pin." :
                        username + " commented on a pin near " + location;
                holder.icon.setImageResource(R.drawable.ic_comment);
                break;
            case FOLLOW:
                holder.icon.setImageResource(R.drawable.ic_follow);
                if (firebase.getCurrentUser().getUid().equals(id)) {
                    textContents = username + " followed you";
                } else {
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
                }
                break;
        }
        if (!textContents.isEmpty()) holder.text.setText(textContents);

        // Clicking on the picture of the action's author will navigate to their profile
        holder.image.setOnClickListener(view -> navController.navigate(
                NavigationDirections.profile().setUid(action.getAuthor())));

        // Clicking on the card will navigate to the action's relevant page
        holder.item.setOnClickListener(view -> {
            if (type == ActivityItem.ActivityType.DROP || type == ActivityItem.ActivityType.FIND || type == ActivityItem.ActivityType.COMMENT) {
                if (firebase.getCachedFoundPinMetadata()
                        .contains(id) || firebase.getCachedDroppedPinMetadata().contains(id))
                    navController.navigate(NavigationDirections.pinView(id));
                else Toast.makeText(parentContext, R.string.undiscovered_pin_locked,
                        Toast.LENGTH_SHORT).show();

            } else if (type == ActivityItem.ActivityType.FOLLOW) {
                navController.navigate(NavigationDirections.profile().setUid(id));
            }
        });
    }

    public enum FeedSource {
        FEED, PROFILE
    }

    // View Holder Class to handle Recycler View.
    public static class FeedViewHolder extends RecyclerView.ViewHolder {
        private final CardView item;
        private final ImageView image;
        private final TextView text;
        private final TextView timestamp;
        private final ImageView icon;


        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.feed_item);
            image = itemView.findViewById(R.id.feed_item_image);
            text = itemView.findViewById(R.id.feed_item_text);
            timestamp = itemView.findViewById(R.id.feed_item_time);
            icon = itemView.findViewById(R.id.feed_item_icon);
        }
    }
}