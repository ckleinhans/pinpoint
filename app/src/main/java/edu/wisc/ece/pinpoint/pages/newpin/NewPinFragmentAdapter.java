package edu.wisc.ece.pinpoint.pages.newpin;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class NewPinFragmentAdapter extends FragmentStateAdapter {

    private final int tabCount;
    private NewPinImageContentFragment imageContentFragment;

    public NewPinFragmentAdapter(@NonNull FragmentManager fragmentManager, int tabs,
                                 @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
        tabCount = tabs;
    }

    public NewPinImageContentFragment getImageContentFragment() {
        return imageContentFragment;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new NewPinTextContentFragment();
        } else {
            imageContentFragment = new NewPinImageContentFragment();
            return imageContentFragment;
        }
    }
}
