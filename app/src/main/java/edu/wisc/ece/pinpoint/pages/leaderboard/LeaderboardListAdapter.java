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
import edu.wisc.ece.pinpoint.utils.FormatUtils;

public class LeaderboardListAdapter
        extends RecyclerView.Adapter<LeaderboardListAdapter.LeaderboardViewHolder> {
    private static final int GOLD = 0xFFFFD700;
    private static final int SILVER = 0xFFC0C0C0;
    private static final int BRONZE = 0xFFCD7F32;
    private final List<String> userIds;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private final Fragment fragment;
    private final LeaderboardListFragment.LeaderboardListType listType;

    // TODO: make this adapter a ListAdapter to improve UX & performance
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
            holder.stat.setText(FormatUtils.trimmedNumber(user.getNumPinsFound()));
        } else {
            holder.stat.setText(FormatUtils.trimmedNumber(user.getNumPinsDropped()));
        }

        String rankString = String.valueOf(position + 1);
        if (rankString.length() > 3) {
            holder.rank.setTextSize(25);
            if (rankString.length() > 4) {
                holder.rank.setText(FormatUtils.trimmedNumber(position + 1));
            } else {
                holder.rank.setText(rankString);
            }
        } else {
            holder.rank.setTextSize(30);
            holder.rank.setText(rankString);
        }
        holder.item.setOnClickListener(
                view -> navController.navigate(NavigationDirections.profile().setUid(userId)));

        if (position < 3) {
            holder.rankBackground.setVisibility(View.VISIBLE);
            if (position == 0) holder.rankBackground.getDrawable().setTint(GOLD);
            else if (position == 1) holder.rankBackground.getDrawable().setTint(SILVER);
            else holder.rankBackground.getDrawable().setTint(BRONZE);
        }
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }

    // View Holder Class to handle Recycler View.
    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        private final CardView item;
        private final ImageView image;
        private final TextView rank;
        private final ImageView rankBackground;
        private final TextView username;
        private final TextView stat;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.leaderboard_item);
            image = itemView.findViewById(R.id.leaderboard_item_image);
            rank = itemView.findViewById(R.id.leaderboard_item_rank);
            rankBackground = itemView.findViewById(R.id.leaderboard_item_rank_background);
            username = itemView.findViewById(R.id.leaderboard_item_username);
            stat = itemView.findViewById(R.id.leaderboard_item_stat);
        }
    }
}