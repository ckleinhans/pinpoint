package edu.wisc.ece.pinpoint.pages.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class UserListFragment extends Fragment {
    private FirebaseDriver firebase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);
        TextView title = view.findViewById(R.id.user_list_title);
        ImageButton backButton = view.findViewById(R.id.user_list_back);

        backButton.setOnClickListener(v -> navController.popBackStack());

        // Initialize recycler view with empty adapter
        RecyclerView recyclerView = view.findViewById(R.id.user_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<String> userIds = new ArrayList<>();
        UserListAdapter adapter = new UserListAdapter(userIds, navController, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // Get list type from arguments
        UserListFragmentArgs args = UserListFragmentArgs.fromBundle(requireArguments());
        String uid = args.getUid();
        if (args.getUserListType() == UserListType.FOLLOWERS) {
            title.setText(R.string.followers_text);
            if (firebase.getCachedFollowers(uid) != null) {
                userIds.addAll(firebase.getCachedFollowers(uid));
                adapter.notifyItemRangeInserted(0, userIds.size());
            } else {
                // Need to fetch data to populate adapter
                firebase.fetchFollowers(uid).addOnSuccessListener(users -> {
                    userIds.addAll(users);
                    adapter.notifyItemRangeInserted(0, userIds.size());
                });
            }
        } else {
            title.setText(R.string.following_text);
            if (firebase.getCachedFollowing(uid) != null) {
                userIds.addAll(firebase.getCachedFollowing(uid));
                adapter.notifyItemRangeInserted(0, userIds.size());
            } else {
                // Need to fetch data to populate adapter
                firebase.fetchFollowing(uid).addOnSuccessListener(users -> {
                    userIds.addAll(users);
                    adapter.notifyItemRangeInserted(0, userIds.size());
                });
            }
        }
    }

    public enum UserListType {
        FOLLOWING, FOLLOWERS
    }
}
