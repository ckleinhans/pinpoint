package edu.wisc.ece.pinpoint.pages.pins;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

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
        return position == 0 ?  new PinFoundFragment() : new PinDroppedFragment() ;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
