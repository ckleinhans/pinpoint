package edu.wisc.ece.pinpoint.pages.feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import edu.wisc.ece.pinpoint.data.ActivityList;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class FeedFragment extends Fragment {
    public static final String UID_ARG_KEY = "uid";
    private FirebaseDriver firebase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.feed_recycler_view);
        ConstraintLayout topBar = view.findViewById(R.id.top_bar);
        // Scale factor for setting padding in dp
        float scale = getResources().getDisplayMetrics().density;
        NavController navController =
                Navigation.findNavController(requireParentFragment().requireView());

        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(
                new FeedAdapter(new ActivityList(new ArrayList<>()), navController, this,
                        FeedAdapter.FeedSource.FEED));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // Get list type from arguments
        String uid = requireArguments().getString(UID_ARG_KEY);
        if (uid == null) {
            // Show top bar and push down recycler view
            topBar.setVisibility(View.VISIBLE);
            recyclerView.setPadding(0, (int) (56 * scale + 0.5f), 0, (int) (95 * scale + 0.5f));

            // List of tasks to wait for before displaying activity
            List<Task<ActivityList>> fetchTasks = new ArrayList<>();
            // TODO: replace adapter with ListAdapter to improve UX & efficiency
            // Two master lists with activity from all followed users, one is for all cached data
            // to be displayed immediately, the other is for fetching & displaying up to date data
            ActivityList cachedMasterList = new ActivityList(new ArrayList<>());
            ActivityList fetchedMasterList = new ActivityList(new ArrayList<>());
            // For each followed user, check if their activity is cached, then add it to master list
            for (String userId : firebase.getCachedFollowing(firebase.getUid())) {
                ActivityList cachedActivity = firebase.getCachedActivity(userId);
                if (cachedActivity != null) {
                    cachedMasterList.addAll(cachedActivity);
                }
                fetchTasks.add(firebase.fetchActivity(userId)
                        .addOnSuccessListener(fetchedMasterList::addAll).addOnFailureListener(
                                e -> Toast.makeText(requireContext(), R.string.activity_fetch_error,
                                        Toast.LENGTH_SHORT).show()));
            }
            // Setup immediate cached master list
            cachedMasterList.sort();
            recyclerView.setAdapter(new FeedAdapter(cachedMasterList, navController, this,
                    FeedAdapter.FeedSource.FEED));
            // When all fetches done, replace list with up to date data
            Tasks.whenAllComplete(fetchTasks).addOnCompleteListener(activityFetchingComplete -> {
                fetchedMasterList.sort();
                recyclerView.setAdapter(new FeedAdapter(fetchedMasterList, navController, this,
                        FeedAdapter.FeedSource.FEED));
            });

        } else {
            // Hide top bar and push up recycler view
            topBar.setVisibility(View.GONE);
            recyclerView.setPadding(0, 0, 0, (int) (95 * scale + 0.5f));

            // Attempt to use cached activity before fetching
            ActivityList cachedActivity = firebase.getCachedActivity(uid);
            if (cachedActivity != null) recyclerView.setAdapter(
                    new FeedAdapter(cachedActivity, navController, this,
                            FeedAdapter.FeedSource.PROFILE));
            // If user is not self, fetch activity regardless to maintain up to date data
            if (!firebase.getUid().equals(uid) || cachedActivity == null)
                firebase.fetchActivity(uid).addOnSuccessListener(
                        activityList -> recyclerView.setAdapter(
                                new FeedAdapter(activityList, navController, this,
                                        FeedAdapter.FeedSource.PROFILE))).addOnFailureListener(
                        e -> Toast.makeText(requireContext(), R.string.activity_fetch_error,
                                Toast.LENGTH_SHORT).show());
        }

    }
}