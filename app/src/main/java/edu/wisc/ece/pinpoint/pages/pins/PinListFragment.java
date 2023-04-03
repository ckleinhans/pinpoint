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

import java.util.ArrayList;
import java.util.List;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.PinListRecyclerData;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinListFragment extends Fragment {
    private List<PinListRecyclerData> pinListRecyclerData;
    private FirebaseDriver firebase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
        pinListRecyclerData = new ArrayList<>();
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
        NavController navController = Navigation.findNavController(view);

        // Get pin IDs from arguments
        Bundle args = requireArguments();
        for (String pid : args.getStringArrayList("pinIds")) {
            pinListRecyclerData.add(new PinListRecyclerData(pid, firebase.getCachedPin(pid)));
        }

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(new PinListAdapter(pinListRecyclerData, navController));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }
}
