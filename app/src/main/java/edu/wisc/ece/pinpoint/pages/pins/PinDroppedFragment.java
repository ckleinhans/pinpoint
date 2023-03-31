package edu.wisc.ece.pinpoint.pages.pins;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Map;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.pages.pins.PinListAdapter;
import edu.wisc.ece.pinpoint.pages.pins.RecyclerData;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinDroppedFragment extends Fragment {

    private NavController navController;
    private FirebaseDriver firebase;
    ArrayList<edu.wisc.ece.pinpoint.pages.pins.RecyclerData> recyclerDataArrayList;
    RecyclerView recyclerView;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseDriver.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pin_dropped_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        recyclerDataArrayList = new ArrayList<>();

        NavHostFragment navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager()
                .findFragmentById(R.id.activity_main_nav_host_fragment);
        navController = navHostFragment.getNavController();


        // added data to array list
        firebase.getDroppedPins().addOnCompleteListener(task -> setPinData(task.getResult()));

        recyclerView =  (RecyclerView) view.findViewById(R.id.pinlist_recycler_view);


    }

    public void setPinData(Map<String, Pin> pins){
        for(Map.Entry<String, Pin> entry : pins.entrySet()) {
            recyclerDataArrayList.add(new RecyclerData(entry.getKey(), entry.getValue()));
        }
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);

        edu.wisc.ece.pinpoint.pages.pins.PinListAdapter adapter = new PinListAdapter(recyclerDataArrayList, navController);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }
}
