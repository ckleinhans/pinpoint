package edu.wisc.ece.pinpoint.pages.pins;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.OrderedHashSet;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinListFragment extends Fragment {
    public static final String LIST_TYPE_ARG_KEY = "pinListType";
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
        return inflater.inflate(R.layout.fragment_pin_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.pinlist_recycler_view);
        NavController navController =
                Navigation.findNavController(requireParentFragment().requireView());

        // Get list type from arguments
        PinListType listType = PinListType.valueOf(requireArguments().getString(LIST_TYPE_ARG_KEY));
        OrderedHashSet<String> pinIds = null;
        switch (listType) {
            case USER:
                // TODO: make user pin list pages dynamically based on UID argument
                pinIds = firebase.getCachedDroppedPinIds();
                break;
            case DROPPED:
                pinIds = firebase.getCachedDroppedPinIds();
                break;
            case FOUND:
                pinIds = firebase.getCachedFoundPinIds();
                break;
        }

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(new PinListAdapter(pinIds, navController));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    public enum PinListType {
        FOUND, DROPPED, USER
    }
}
