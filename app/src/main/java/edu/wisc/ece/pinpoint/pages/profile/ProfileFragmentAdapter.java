package edu.wisc.ece.pinpoint.pages.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import edu.wisc.ece.pinpoint.pages.pins.PinListFragment;

public class ProfileFragmentAdapter extends FragmentStateAdapter {

    private final int tabCount;
    private final NavController navController;
    private final String uid;

    public ProfileFragmentAdapter(@NonNull FragmentManager fragmentManager, int tabCount,
                                  @NonNull Lifecycle lifecycle, NavController navController,
                                  String uid) {
        super(fragmentManager, lifecycle);
        this.tabCount = tabCount;
        this.navController = navController;
        this.uid = uid;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new ActivityFragment();
        } else {
            PinListFragment frag = new PinListFragment(navController);
            Bundle args = new Bundle();
            args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
                    PinListFragment.PinListType.USER.name());
            args.putString(PinListFragment.UID_ARG_KEY, uid);
            frag.setArguments(args);
            return frag;
        }
    }
}
