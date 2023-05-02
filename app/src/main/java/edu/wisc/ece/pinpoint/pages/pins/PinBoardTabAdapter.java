package edu.wisc.ece.pinpoint.pages.pins;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class PinBoardTabAdapter extends FragmentStateAdapter {
    private final int tabCount;

    public PinBoardTabAdapter(@NonNull FragmentManager fragmentManager, int tabCount,
                              @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
        this.tabCount = tabCount;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        PinListFragment frag = new PinListFragment();
        Bundle args = new Bundle();
        switch (position) {
            case 0:
                args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
                        PinListFragment.PinListType.ALL.name());
                break;
            case 1:
                args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
                        PinListFragment.PinListType.FOLLOWED.name());
                break;
            case 2:
                args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
                        PinListFragment.PinListType.NFC.name());
                break;
            case 3:
                args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
                        PinListFragment.PinListType.LANDMARK.name());
                break;
            case 4:
                args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
                        PinListFragment.PinListType.OTHER.name());
                break;
        }
        frag.setArguments(args);
        return frag;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
