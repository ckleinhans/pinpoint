package edu.wisc.ece.pinpoint.pages.leaderboard;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class LeaderboardTabAdapter extends FragmentStateAdapter {
    private final int tabCount;

    public LeaderboardTabAdapter(@NonNull FragmentManager fragmentManager, int tabCount,
                                 @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
        this.tabCount = tabCount;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // TODO: make leaderboard list fragments
//        PinListFragment frag = new PinListFragment();
//        Bundle args = new Bundle();
//        if (position == 0) {
//            args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
//                    PinListFragment.PinListType.FOUND.name());
//        } else {
//            args.putString(PinListFragment.LIST_TYPE_ARG_KEY,
//                    PinListFragment.PinListType.DROPPED.name());
//        }
//        frag.setArguments(args);
        return null;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
