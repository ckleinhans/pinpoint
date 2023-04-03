package edu.wisc.ece.pinpoint.pages.pins;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinBoardTabAdapter extends FragmentStateAdapter {
    private final int tabCount;

    public PinBoardTabAdapter(@NonNull FragmentManager fragmentManager, int tabs,
                              @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
        tabCount = tabs;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        FirebaseDriver firebase = FirebaseDriver.getInstance();
        PinListFragment frag = new PinListFragment();
        Bundle args = new Bundle();
        if (position == 0) {
            args.putStringArrayList("pinIds", new ArrayList<>(firebase.getCachedFoundPinIds()));
        } else {
            args.putStringArrayList("pinIds", new ArrayList<>(firebase.getCachedDroppedPinIds()));
        }
        frag.setArguments(args);
        return frag;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
