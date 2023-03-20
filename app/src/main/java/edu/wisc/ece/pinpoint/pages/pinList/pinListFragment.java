package edu.wisc.ece.pinpoint.pages.pinList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import edu.wisc.ece.pinpoint.R;

public class pinListFragment extends Fragment {
    ImageButton button1;
    NavController navController;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pin_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (Build.VERSION.SDK_INT > 25) {
            button1 = requireView().findViewById(R.id.imageButton);
            button1.setTooltipText("PIN 1 with details");
            navController = Navigation.findNavController(view);
            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openPinView();
                }
            });

        }
    }

    public void openPinView(){
      NavDirections directions =  pinListFragmentDirections.actionNavbarSearchToPinViewPage();
        navController.navigate(directions);

    }
}