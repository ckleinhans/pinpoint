package edu.wisc.ece.pinpoint.pages.leaderboard;

import android.os.Bundle;

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
        LeaderboardListFragment frag = new LeaderboardListFragment();
        Bundle args = new Bundle();
        if (position == 0) {
            args.putString(LeaderboardListFragment.LIST_TYPE_ARG_KEY,
                    LeaderboardListFragment.LeaderboardListType.FOUND.name());
        } else {
            args.putString(LeaderboardListFragment.LIST_TYPE_ARG_KEY,
                    LeaderboardListFragment.LeaderboardListType.DROPPED.name());
        }
        frag.setArguments(args);
        return frag;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
