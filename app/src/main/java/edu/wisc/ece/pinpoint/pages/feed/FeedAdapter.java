package edu.wisc.ece.pinpoint.pages.feed;

import android.content.Context;
import android.util.TypedValue;
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

    // TODO: make this adapter a ListAdapter to improve UX & performance
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

        if (firebase.isUserCached(author)) {
            setContents(firebase.getCachedUser(author), action, holder);
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

        if (author != null) author.loadProfilePic(holder.image, fragment);

        // Detect if the activity belongs to current user
        String username =
                action.getAuthor().equals(firebase.getUid()) ? fragment.getString(R.string.you) :
                        author != null ? author.getUsername() :
                                fragment.getString(R.string.deleted_user);
        String location = FormatUtils.formattedActivityLocation(action.getBroadLocationName(),
                action.getNearbyLocationName());
        switch (type) {
            case DROP:
                holder.text.setText(
                        String.format(fragment.getString(R.string.activity_drop_text), username,
                                location));
                holder.icon.setImageResource(R.drawable.ic_drop);
                break;
            case FIND:
                holder.text.setText(
                        String.format(fragment.getString(R.string.activity_find_text), username,
                                location));
                holder.icon.setImageResource(R.drawable.ic_search);
                break;
            case COMMENT:
                holder.text.setText(
                        String.format(fragment.getString(R.string.activity_comment_text), username,
                                location));
                holder.icon.setImageResource(R.drawable.ic_comment);
                break;
            case FOLLOW:
                holder.icon.setImageResource(R.drawable.ic_follow);
                if (firebase.isUserCached(id)) {
                    setFollowActivityContents(holder, firebase.getCachedUser(id), id, username);
                } else {
                    firebase.fetchUser(id).addOnCompleteListener(
                            task -> setFollowActivityContents(holder, task.getResult(), id,
                                    username));
                }
                break;
        }

        // Clicking author's picture will navigate to their profile unless already there
        if (source != FeedSource.PROFILE && author != null) holder.image.setOnClickListener(
                view -> navController.navigate(
                        NavigationDirections.profile(action.getAuthor())));

        if (type != ActivityItem.ActivityType.FOLLOW) {
            // Reset color if error
            TypedValue typedValue = new TypedValue();
            parentContext.getTheme()
                    .resolveAttribute(com.google.android.material.R.attr.colorOnBackground,
                            typedValue, true);
            holder.text.setTextColor(typedValue.data);
            // Clicking on the card will navigate to the action's relevant page
            holder.item.setOnClickListener(view -> {
                if (firebase.getCachedFoundPinMetadata()
                        .contains(id) || firebase.getCachedDroppedPinMetadata().contains(id))
                    navController.navigate(NavigationDirections.pinView(id));
                else Toast.makeText(parentContext, R.string.feed_pin_deleted_or_undiscovered,
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setFollowActivityContents(FeedViewHolder holder, User target, String targetUID,
                                           String username) {
        if (target != null) {
            String targetUsername = targetUID.equals(firebase.getUid()) ?
                    fragment.getString(R.string.you_lowercase) : target.getUsername();
            holder.text.setText(
                    String.format(fragment.getString(R.string.activity_follow_text), username,
                            targetUsername));
            TypedValue typedValue = new TypedValue();
            parentContext.getTheme()
                    .resolveAttribute(com.google.android.material.R.attr.colorOnBackground,
                            typedValue, true);
            holder.text.setTextColor(typedValue.data);
            holder.item.setOnClickListener(view -> navController.navigate(
                    NavigationDirections.profile(targetUID)));
        } else {
            holder.text.setText(
                    String.format(fragment.getString(R.string.activity_follow_text), username,
                            fragment.getString(R.string.deleted_user)));
            TypedValue typedValue = new TypedValue();
            parentContext.getTheme()
                    .resolveAttribute(com.google.android.material.R.attr.colorError, typedValue,
                            true);
            holder.text.setTextColor(typedValue.data);
            holder.item.setOnClickListener(null);
        }
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