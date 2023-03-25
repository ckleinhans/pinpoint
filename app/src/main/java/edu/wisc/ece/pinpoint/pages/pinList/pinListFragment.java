package edu.wisc.ece.pinpoint.pages.pinList;

//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.navigation.NavController;
//import androidx.navigation.NavDirections;
//import androidx.navigation.Navigation;
//
//import android.content.Intent;
//import android.os.Build;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageButton;
//
//import edu.wisc.ece.pinpoint.R;
//
//public class pinListFragment extends Fragment {
//    ImageButton button1;
//    NavController navController;
//
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_pin_list, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        if (Build.VERSION.SDK_INT > 25) {
//            button1 = requireView().findViewById(R.id.imageButton);
//            button1.setTooltipText("PIN 1 with details");
//            navController = Navigation.findNavController(view);
//            button1.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    openPinView();
//                }
//            });
//
//        }
//    }
//
//    public void openPinView(){
//      NavDirections directions =  pinListFragmentDirections.actionNavbarSearchToPinViewPage();
//        navController.navigate(directions);
//
//    }
//}

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


    private ArrayList<RecyclerData> recyclerDataArrayList;

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_pin_list, container, false);


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
