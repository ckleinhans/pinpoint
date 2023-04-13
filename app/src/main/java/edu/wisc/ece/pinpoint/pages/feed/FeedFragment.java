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
        // Get list type from arguments
        String uid = requireArguments().getString(UID_ARG_KEY);
        if (uid == null) {

        }
        else {
            firebase.fetchActivity(uid).addOnSuccessListener(task -> setupRecyclerView(view, task));
        }

    }
    private void setupRecyclerView(View view, ActivityList activity) {
        RecyclerView recyclerView = view.findViewById(R.id.feed_recycler_view);
        NavController navController =
                Navigation.findNavController(requireParentFragment().requireView());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new FeedAdapter(activity, navController, this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }
}