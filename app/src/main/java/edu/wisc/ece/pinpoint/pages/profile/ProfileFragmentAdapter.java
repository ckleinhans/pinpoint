package edu.wisc.ece.pinpoint.pages.profile;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ProfileFragmentAdapter extends FragmentStateAdapter {

    private final int tabCount;

    public ProfileFragmentAdapter(@NonNull FragmentManager fragmentManager, int tabs,
                                  @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
        tabCount = tabs;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return position == 0 ? new ActivityFragment() : new DroppedPinsFragment();
    }
}
