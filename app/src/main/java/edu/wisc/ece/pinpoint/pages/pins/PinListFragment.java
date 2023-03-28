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
import edu.wisc.ece.pinpoint.R;


public class PinListFragment extends Fragment {


    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavHostFragment navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager()
                .findFragmentById(R.id.activity_main_nav_host_fragment);
        navController = navHostFragment.getNavController();
        ArrayList<RecyclerData> recyclerDataArrayList = new ArrayList<>();

        // added data to array list
        recyclerDataArrayList.add(new RecyclerData("DSA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("D", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DSA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("D", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DSA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("D", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DSA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("D", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DSA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("D", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DSA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("D", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DSA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("D", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DSA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("DA", R.drawable.oldnote));
        recyclerDataArrayList.add(new RecyclerData("D", R.drawable.oldnote));

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.pinlist_recycler_view);



        PinListAdapter adapter = new PinListAdapter(recyclerDataArrayList, navController);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());


    }
}
