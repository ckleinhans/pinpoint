package edu.wisc.ece.pinpoint.pages.pins;

import android.os.Bundle;
import android.util.Log;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.OrderedPinMetadata;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinListFragment extends Fragment {
    public static final String LIST_TYPE_ARG_KEY = "pinListType";
    public static final String UID_ARG_KEY = "uid";
    private static final String TAG = PinListFragment.class.getName();
    private FirebaseDriver firebase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get list type from arguments
        PinListType listType = PinListType.valueOf(requireArguments().getString(LIST_TYPE_ARG_KEY));
        OrderedPinMetadata pinMetadata;
        switch (listType) {
            case USER:
                String uid = requireArguments().getString(UID_ARG_KEY);
                if (firebase.getCachedUserPinMetadata(uid) == null) {
                    firebase.fetchUserPins(uid).addOnSuccessListener(
                                    metadata -> setupRecyclerView(view, metadata, listType))
                            .addOnFailureListener(e -> {
                                Log.w(TAG, e);
                                Toast.makeText(requireContext(), R.string.pin_fetch_error,
                                        Toast.LENGTH_SHORT).show();
                            });
                } else {
                    pinMetadata = firebase.getCachedUserPinMetadata(uid);
                    setupRecyclerView(view, pinMetadata, listType);
                }
                break;
            case DROPPED:
                pinMetadata = firebase.getCachedDroppedPinMetadata();
                setupRecyclerView(view, pinMetadata, listType);
                break;
            case FOUND:
                pinMetadata = firebase.getCachedFoundPinMetadata();
                setupRecyclerView(view, pinMetadata, listType);
                break;
        }
    }

    private void setupRecyclerView(View view, OrderedPinMetadata pinMetadata,
                                   PinListType listType) {
        RecyclerView recyclerView = view.findViewById(R.id.pinlist_recycler_view);
        NavController navController =
                Navigation.findNavController(requireParentFragment().requireView());

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(new PinListAdapter(pinMetadata, navController, listType));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    public enum PinListType {
        FOUND, DROPPED, USER
    }
}
