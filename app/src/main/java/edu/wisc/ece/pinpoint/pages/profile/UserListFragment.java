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
import java.util.List;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class UserListFragment extends Fragment {
    private static final String LIST_TYPE_ARG_KEY = "userListType";
    private static final String UID_ARG_KEY = "uid";
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
        recyclerView.setAdapter(new UserListAdapter(new ArrayList<>(), navController, this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // Get list type from arguments
        UserListFragmentArgs args = UserListFragmentArgs.fromBundle(requireArguments());
        String uid = args.getUid();
        if (args.getUserListType() == UserListType.FOLLOWERS) {
            title.setText(R.string.followers_text);
            if (firebase.getCachedFollowers(uid) != null) {
                List<String> userIds = new ArrayList<>(firebase.getCachedFollowers(uid));
                recyclerView.setAdapter(new UserListAdapter(userIds, navController, this));
            } else {
                // Need to fetch data to populate adapter
                firebase.fetchFollowers(uid).addOnSuccessListener(users -> {
                    List<String> userIds = new ArrayList<>(users);
                    recyclerView.setAdapter(new UserListAdapter(userIds, navController, this));
                });
            }
        } else {
            title.setText(R.string.following_text);
            if (firebase.getCachedFollowing(uid) != null) {
                List<String> userIds = new ArrayList<>(firebase.getCachedFollowing(uid));
                recyclerView.setAdapter(new UserListAdapter(userIds, navController, this));
            } else {
                // Need to fetch data to populate adapter
                firebase.fetchFollowing(uid).addOnSuccessListener(users -> {
                    List<String> userIds = new ArrayList<>(users);
                    recyclerView.setAdapter(new UserListAdapter(userIds, navController, this));
                });
            }
        }
    }

    public enum UserListType {
        FOLLOWING, FOLLOWERS
    }
}
