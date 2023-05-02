package edu.wisc.ece.pinpoint.pages.leaderboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class LeaderboardListFragment extends Fragment {
    public static final String LIST_TYPE_ARG_KEY = "leaderboardListType";
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

        // Initialize recycler view with empty adapter
        RecyclerView recyclerView = view.findViewById(R.id.leaderboard_recycler_view);
        NavController navController =
                Navigation.findNavController(requireParentFragment().requireView());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<String> userIds = new ArrayList<>();
        String uid = firebase.getUid();
        userIds.add(uid);
        LeaderboardListAdapter adapter =
                new LeaderboardListAdapter(userIds, navController, this, listType);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // Get list type from arguments
        listType = LeaderboardListType.valueOf(requireArguments().getString(LIST_TYPE_ARG_KEY));
        List<Task<Void>> fetchTasks = new ArrayList<>();
        for (String userId : firebase.getCachedFollowing(uid)) {
            if (firebase.getCachedUser(userId) == null) {
                fetchTasks.add(firebase.fetchUser(userId).continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(requireContext(), R.string.user_fetch_error_message,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Successfully fetched user, add to list
                        userIds.add(userId);
                    }
                    return null;
                }));
            } else {
                // User already cached, add user ID to list
                userIds.add(userId);
            }
        }
        Tasks.whenAllComplete(fetchTasks).addOnCompleteListener(task -> {
            userIds.sort((uid1, uid2) -> {
                User user1 = firebase.getCachedUser(uid1);
                User user2 = firebase.getCachedUser(uid2);
                if (listType == LeaderboardListType.FOUND) {
                    return user2.getNumPinsFound() - user1.getNumPinsFound();
                } else {
                    return user2.getNumPinsDropped() - user1.getNumPinsDropped();
                }
            });
            //noinspection NotifyDataSetChanged
            adapter.notifyDataSetChanged();
        });
    }

    public enum LeaderboardListType {
        FOUND, DROPPED
    }
}
