package edu.wisc.ece.pinpoint.pages.pinList;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import androidx.navigation.NavController;

import edu.wisc.ece.pinpoint.R;

public class pinListFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;
    NavController navController;


    private ArrayList<RecyclerData> recyclerDataArrayList;

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_pin_list, container, false);
        View root = inflater.inflate(R.layout.recycler_view, container, false);


            recyclerDataArrayList=new ArrayList<>();

            // added data to array list
            recyclerDataArrayList.add(new RecyclerData("DSA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("D",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DSA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("D",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DSA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("D",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DSA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("D",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DSA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("D",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DSA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("D",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DSA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("D",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DSA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("DA",R.drawable.oldnote));
            recyclerDataArrayList.add(new RecyclerData("D",R.drawable.oldnote));


            GridLayoutManager layoutManager=new GridLayoutManager(getContext(),3);


            recyclerView = (RecyclerView) rootView.findViewById(R.id.idCourseRV);
            adapter = new RecyclerViewAdapter(recyclerDataArrayList);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);
            recyclerView.setItemAnimator(new DefaultItemAnimator());



        return rootView;
    }




    }
