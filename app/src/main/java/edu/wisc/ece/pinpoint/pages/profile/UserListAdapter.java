package edu.wisc.ece.pinpoint.pages.profile;

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

import java.util.List;

import edu.wisc.ece.pinpoint.NavigationDirections;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserListViewHolder> {
    private final List<String> userIds;
    private final NavController navController;
    private final FirebaseDriver firebase;
    private final Fragment fragment;

    public UserListAdapter(List<String> userIds, NavController navController, Fragment fragment) {
        this.userIds = userIds;
        this.navController = navController;
        this.fragment = fragment;
        firebase = FirebaseDriver.getInstance();
    }

    @NonNull
    @Override
    public UserListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_user_list_item, parent, false);
        return new UserListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserListViewHolder holder, int position) {
        String userId = userIds.get(position);
        if (firebase.isUserCached(userId)) {
            setUserData(holder, userId, firebase.getCachedUser(userId));
        } else {
            firebase.fetchUser(userId)
                    .addOnSuccessListener(user -> setUserData(holder, userId, user))
                    .addOnFailureListener(e -> Toast.makeText(fragment.getContext(),
                            R.string.user_fetch_error_message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }

    private void setUserData(@NonNull UserListViewHolder holder, String userId, User user) {
        if (user != null) {
            user.loadProfilePic(holder.image, fragment);
            holder.username.setText(user.getUsername());
            if (fragment.getContext() != null) {
                TypedValue typedValue = new TypedValue();
                fragment.getContext().getTheme()
                        .resolveAttribute(com.google.android.material.R.attr.colorOnBackground,
                                typedValue, true);
                holder.username.setTextColor(typedValue.data);
            }
            holder.item.setOnClickListener(
                    view -> navController.navigate(NavigationDirections.profile(userId)));
        } else {
            holder.username.setText(R.string.deleted_user);
            if (fragment.getContext() != null) {
                TypedValue typedValue = new TypedValue();
                fragment.getContext().getTheme()
                        .resolveAttribute(com.google.android.material.R.attr.colorError, typedValue,
                                true);
                holder.username.setTextColor(typedValue.data);
            }
            holder.image.setOnClickListener(null);
        }
    }

    // View Holder Class to handle Recycler View.
    public static class UserListViewHolder extends RecyclerView.ViewHolder {
        private final CardView item;
        private final ImageView image;
        private final TextView username;

        public UserListViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.user_list_item);
            image = itemView.findViewById(R.id.user_list_item_image);
            username = itemView.findViewById(R.id.user_list_item_username);
        }
    }
}