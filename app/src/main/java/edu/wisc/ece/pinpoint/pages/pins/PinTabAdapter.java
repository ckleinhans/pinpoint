package edu.wisc.ece.pinpoint.pages.pins;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import edu.wisc.ece.pinpoint.pages.profile.ActivityFragment;
import edu.wisc.ece.pinpoint.pages.profile.DroppedPinsFragment;
import edu.wisc.ece.pinpoint.pages.profile.EditProfileFragment;

public class PinTabAdapter extends FragmentStateAdapter {
    private final int tabCount;

    public PinTabAdapter(@NonNull FragmentManager fragmentManager, int tabs,
                                  @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
        tabCount = tabs;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return position == 0 ?  new PinListFragment() : new DroppedPinsFragment() ;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
