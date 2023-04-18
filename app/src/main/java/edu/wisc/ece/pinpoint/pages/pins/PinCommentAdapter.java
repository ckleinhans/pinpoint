package edu.wisc.ece.pinpoint.pages.pins;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Comment;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinCommentAdapter extends RecyclerView.Adapter<PinCommentAdapter.PinCommentViewHolder> {

    private final FirebaseDriver firebase;
    private final NavController navController;
    private final ArrayList<Comment> comments;
    private Context parentContext;

    public PinCommentAdapter(ArrayList<Comment> comments, NavController navController) {
        firebase = FirebaseDriver.getInstance();
        this.navController = navController;
        this.comments = comments;
    }

    @NonNull
    @Override
    public PinCommentAdapter.PinCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_comment_item, parent, false);
        parentContext = parent.getContext();
        return new PinCommentAdapter.PinCommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PinCommentAdapter.PinCommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        User user = FirebaseDriver.getInstance().getCachedUser(comment.getAuthorUID());
        System.out.println(comment.getAuthorUID());
        holder.content.setText(comment.getContent());
        holder.timestamp.setText(comment.getTimestamp().toString());
        holder.username.setText(user.getUsername());
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    public static class PinCommentViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView item;
        private final ImageView image;
        private final TextView content;
        private final TextView username;
        private final TextView timestamp;

        public PinCommentViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.comment_item);
            image = itemView.findViewById(R.id.comment_profile_image);
            content = itemView.findViewById(R.id.comment_content);
            username = itemView.findViewById(R.id.comment_username_text);
            timestamp = itemView.findViewById(R.id.comment_timestamp);
        }
    }
}
