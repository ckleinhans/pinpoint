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

import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;

import edu.wisc.ece.pinpoint.NavigationDirections;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.ActivityItem;
import edu.wisc.ece.pinpoint.data.ActivityList;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
    private final ActivityList activity;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private Context parentContext;
    private final Fragment fragment;

    public FeedAdapter(ActivityList activity, NavController navController, Fragment fragment) {
        this.activity = activity;
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
        // Single action in activity list, reverse order
        ActivityItem action = activity.get(activity.size() - position - 1);
        String id = action.getId();
        String author = action.getAuthor();
        ActivityItem.ActivityType type = action.getType();
        Date time = action.getTimestamp();
        boolean isCurrentUser = author.equals(FirebaseAuth.getInstance().getUid());
        // Set timestamp
        holder.timestamp.setText(FormatUtils.formattedDateTime(time));
        // Clicking on the picture of the action's author will navigate to their profile
        holder.image.setOnClickListener(view ->
                navController.navigate(NavigationDirections.profile().setUid(author)));
        // Clicking on the card will navigate to the action's relevant page
        holder.item.setOnClickListener(view -> {
            if (type == ActivityItem.ActivityType.DROP || type == ActivityItem.ActivityType.FIND ||
                    type == ActivityItem.ActivityType.COMMENT) {
                if (firebase.getCachedFoundPinMetadata()
                        .contains(id) || firebase.getCachedDroppedPinMetadata().contains(id))
                    navController.navigate(NavigationDirections.pinView(id));
                else
                    Toast.makeText(parentContext, R.string.undiscovered_pin_locked,
                                    Toast.LENGTH_SHORT).show();

            } else if (type == ActivityItem.ActivityType.FOLLOW) {
                navController.navigate(NavigationDirections.profile().setUid(id));
            }
        });
        User cachedAuthor = firebase.getCachedUser(author);
        if (cachedAuthor != null) {
            // put author data in feed item
            setContents(cachedAuthor, type, id, holder, isCurrentUser);
        } else {
            // Since only using author for profile pic & username, only fetch if not cached
            firebase.fetchUser(author)
                    .addOnCompleteListener(task -> setContents(task.getResult(), type, id, holder, isCurrentUser));
        }
    }

    private void setContents(User author, ActivityItem.ActivityType type, String id, @NonNull FeedViewHolder holder, boolean isCurrentUser){
        author.loadProfilePic(holder.image, fragment);
        // Detect if the activity belongs to current user, and trim if username is too long
        String username = isCurrentUser ? "You" :
                author.getUsername().length() > 12 ? author.getUsername().substring(0, 9)+"..." :
                        author.getUsername();
        String textContents = "";
        switch(type){
            case DROP:
                textContents = username + " dropped a pin";
                holder.icon.setImageResource(R.drawable.ic_drop);
                break;
            case FIND:
                textContents = username + " found a pin";
                holder.icon.setImageResource(R.drawable.ic_search);
                break;
            case COMMENT:
                textContents = username + " commented on a pin";
                holder.icon.setImageResource(R.drawable.ic_comment);
                break;
            case FOLLOW:
                holder.icon.setImageResource(R.drawable.ic_follow);
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