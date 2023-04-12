package edu.wisc.ece.pinpoint.pages.feed;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.OrderedPinMetadata;
import edu.wisc.ece.pinpoint.pages.pins.PinListAdapter;
import edu.wisc.ece.pinpoint.pages.pins.PinListFragment;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

// TODO: pass arguments while instantiating to allow use for both Profile & Activity pages
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
        TextView text = view.findViewById(R.id.feed_text);

        // Get list type from arguments
        OrderedPinMetadata pinMetadata;
        String uid = requireArguments().getString(UID_ARG_KEY);
        if (uid == null) {
            text.setText("NAV BAR FEED");
        }
        else {
            text.setText("UID: "+uid);
        }
//        if (firebase.getCachedUserPinMetadata(uid) == null) {
//            firebase.fetchUserPins(uid)
//                    .addOnSuccessListener(metadata -> setupRecyclerView(view, metadata))
//                    .addOnFailureListener(e -> {
//                        Log.w(TAG, e);
//                        Toast.makeText(requireContext(), R.string.pin_fetch_error,
//                                Toast.LENGTH_SHORT).show();
//                    });
//        } else {
//            pinMetadata = firebase.getCachedUserPinMetadata(uid);
//            setupRecyclerView(view, pinMetadata);
//        }
    }
    private void setupRecyclerView(View view, List<HashMap<String, String>> activity) {
        RecyclerView recyclerView = view.findViewById(R.id.pinlist_recycler_view);
        NavController navController =
                Navigation.findNavController(requireParentFragment().requireView());

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(new PinListAdapter(pinMetadata, navController));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }
}