package edu.wisc.ece.pinpoint.pages.pins;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;


public class PinFoundFragment extends Fragment {


    private NavController navController;
    private FirebaseDriver firebase;
    ArrayList<RecyclerData> recyclerDataArrayList;
    RecyclerView recyclerView;


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


        recyclerDataArrayList = new ArrayList<>();

        NavHostFragment navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager()
                .findFragmentById(R.id.activity_main_nav_host_fragment);
        navController = navHostFragment.getNavController();


        // added data to array list
          firebase.getFoundPins().addOnCompleteListener(task -> setPinData(task.getResult()));

        recyclerView =  (RecyclerView) view.findViewById(R.id.pinlist_recycler_view);


    }

    public void setPinData(Map<String, Pin> pins){
        for(Map.Entry<String, Pin> entry : pins.entrySet()) {
            recyclerDataArrayList.add(new RecyclerData(entry.getKey(), entry.getValue()));
        }
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);

        PinListAdapter adapter = new PinListAdapter(recyclerDataArrayList, navController);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }
}
