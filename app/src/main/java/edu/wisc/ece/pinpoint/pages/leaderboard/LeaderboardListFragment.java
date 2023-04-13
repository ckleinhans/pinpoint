package edu.wisc.ece.pinpoint.pages.leaderboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class LeaderboardListFragment extends Fragment {
    public static final String LIST_TYPE_ARG_KEY = "leaderboardListType";
    private static final String TAG = LeaderboardListFragment.class.getName();
    private FirebaseDriver firebase;
    private LeaderboardListType listType;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get list type from arguments
        listType = LeaderboardListType.valueOf(requireArguments().getString(LIST_TYPE_ARG_KEY));
        List<String> userIds = new ArrayList<>();
        // TODO: add all users following
        userIds.add(firebase.getCurrentUser().getUid());
        userIds.add("xXKqynqDzpQTdHcexauUKi1bXnb2");
        userIds.add("Ep3cXdoTQtZXUBofZ4emifyMVdX2");
        userIds.sort((uid1, uid2) -> {
            User user1 = firebase.getCachedUser(uid1);
            User user2 = firebase.getCachedUser(uid2);
            if (listType == LeaderboardListType.FOUND) {
                return user2.getNumPinsFound() - user1.getNumPinsFound();
            } else {
                return user2.getNumPinsDropped() - user1.getNumPinsDropped();
            }
        });
        setupRecyclerView(view, userIds);
    }

    private void setupRecyclerView(View view, List<String> userIds) {
        RecyclerView recyclerView = view.findViewById(R.id.leaderboard_recycler_view);
        NavController navController =
                Navigation.findNavController(requireParentFragment().requireView());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new LeaderboardListAdapter(userIds, navController, this, listType));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    public enum LeaderboardListType {
        FOUND, DROPPED
    }
}
