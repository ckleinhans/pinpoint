package edu.wisc.ece.pinpoint.pages.pins;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class PinBoardTabAdapter extends FragmentStateAdapter {
    private final int tabCount;
    private final NavController navController;

    public PinBoardTabAdapter(@NonNull FragmentManager fragmentManager, int tabCount,
                              @NonNull Lifecycle lifecycle, NavController navController) {
        super(fragmentManager, lifecycle);
        this.tabCount = tabCount;
        this.navController = navController;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        PinListFragment frag = new PinListFragment(navController);
        Bundle args = new Bundle();
        if (position == 0) {
            args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
                    PinListFragment.PinListType.FOUND.name());
        } else {
            args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
                    PinListFragment.PinListType.DROPPED.name());
        }
        frag.setArguments(args);
        return frag;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
