package edu.wisc.ece.pinpoint.pages.feed;

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
        NavController navController =
                Navigation.findNavController(requireParentFragment().requireView());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new FeedAdapter(new ActivityList(new ArrayList<>()), navController, this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // Get list type from arguments
        String uid = requireArguments().getString(UID_ARG_KEY);
        if (uid == null) {
            // List of tasks to wait for before displaying activity
            List<Task<ActivityList>> fetchTasks = new ArrayList<>();
            // Master list with activity from all followed users
            ActivityList masterList = new ActivityList(new ArrayList<>());
            // For each followed user, check if their activity is cached, then add it to master list
            firebase.getCachedFollowingIds().forEach((k) -> {
                ActivityList cachedActivity = firebase.getCachedActivity(k);
                if (cachedActivity != null) {
                    masterList.addAll(cachedActivity);
                }
                else fetchTasks.add(firebase.fetchActivity(k).addOnSuccessListener(task ->
                        masterList.addAll(task)));
            });
            Tasks.whenAllComplete(fetchTasks).addOnCompleteListener(activityFetchingComplete -> {
                masterList.sort();
                recyclerView.setAdapter(new FeedAdapter(masterList, navController, this));
            });

        }
        else {
            // Attempt to use cached activity before of fetching
            ActivityList cachedActivity = firebase.getCachedActivity(uid);
            if (cachedActivity != null)
                recyclerView.setAdapter(new FeedAdapter(cachedActivity, navController, this));
            else firebase.fetchActivity(uid).addOnSuccessListener(task ->
                    recyclerView.setAdapter(new FeedAdapter(task, navController, this)));
        }

    }
}