package edu.wisc.ece.pinpoint.pages.pins;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.wisc.ece.pinpoint.NavigationDirections;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Comment;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;

// TODO: make this a ListAdapter
public class PinCommentAdapter
        extends RecyclerView.Adapter<PinCommentAdapter.PinCommentViewHolder> {

    private final FirebaseDriver firebase;
    private final NavController navController;
    private final List<Comment> comments;
    private final Fragment fragment;

    public PinCommentAdapter(List<Comment> comments, NavController navController,
                             Fragment fragment) {
        firebase = FirebaseDriver.getInstance();
        this.navController = navController;
        this.comments = comments;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public PinCommentAdapter.PinCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                     int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_comment_item, parent, false);
        return new PinCommentAdapter.PinCommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PinCommentAdapter.PinCommentViewHolder holder,
                                 int position) {
        Comment comment = comments.get(position);
        holder.content.setText(comment.getContent());
        holder.timestamp.setText(FormatUtils.formattedDate(comment.getTimestamp()));

        String authorUID = comment.getAuthorUID();
        if (firebase.isUserCached(authorUID)) {
            setAuthorData(holder, firebase.getCachedUser(authorUID), authorUID);
        } else {
            firebase.fetchUser(authorUID)
                    .addOnCompleteListener(t -> setAuthorData(holder, t.getResult(), authorUID));
        }
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    private void setAuthorData(PinCommentAdapter.PinCommentViewHolder holder, User author,
                               String authorUID) {
        if (author == null) {
            // user was deleted
            holder.username.setText(R.string.deleted_user);
            holder.username.setTextColor(Color.RED);
        } else {
            holder.username.setText(author.getUsername());
            author.loadProfilePic(holder.image, fragment);
            holder.image.setOnClickListener(view -> navController.navigate(
                    NavigationDirections.profile().setUid(authorUID)));
        }
    }

    public static class PinCommentViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView content;
        private final TextView username;
        private final TextView timestamp;

        public PinCommentViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.comment_profile_image);
            content = itemView.findViewById(R.id.comment_content);
            username = itemView.findViewById(R.id.comment_username_text);
            timestamp = itemView.findViewById(R.id.comment_timestamp);
        }
    }
}
