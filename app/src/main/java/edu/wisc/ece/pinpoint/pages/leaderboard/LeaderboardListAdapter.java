package edu.wisc.ece.pinpoint.pages.leaderboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.wisc.ece.pinpoint.NavigationDirections;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class LeaderboardListAdapter
        extends RecyclerView.Adapter<LeaderboardListAdapter.LeaderboardViewHolder> {
    private final List<String> userIds;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private final Fragment fragment;
    private final LeaderboardListFragment.LeaderboardListType listType;

    public LeaderboardListAdapter(List<String> userIds, NavController navController,
                                  Fragment fragment,
                                  LeaderboardListFragment.LeaderboardListType listType) {
        this.userIds = userIds;
        this.navController = navController;
        this.fragment = fragment;
        this.listType = listType;
        firebase = FirebaseDriver.getInstance();
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_leaderboard_item, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        String userId = userIds.get(position);
        User user = firebase.getCachedUser(userId);
        user.loadProfilePic(holder.image, fragment);
        holder.username.setText(user.getUsername());
        if (listType == LeaderboardListFragment.LeaderboardListType.FOUND) {
            holder.stat.setText(String.valueOf(user.getNumPinsFound()));
        } else {
            holder.stat.setText(String.valueOf(user.getNumPinsDropped()));
        }
        holder.item.setOnClickListener(
                view -> navController.navigate(NavigationDirections.profile().setUid(userId)));
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }

    // View Holder Class to handle Recycler View.
    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        private final CardView item;
        private final ImageView image;
        private final TextView username;
        private final TextView stat;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.leaderboard_item);
            image = itemView.findViewById(R.id.leaderboard_item_image);
            username = itemView.findViewById(R.id.leaderboard_item_username);
            stat = itemView.findViewById(R.id.leaderboard_item_stat);
        }
    }
}