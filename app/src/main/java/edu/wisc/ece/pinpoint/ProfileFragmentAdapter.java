package edu.wisc.ece.pinpoint;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import edu.wisc.ece.pinpoint.fragments.ActivityFragment;
import edu.wisc.ece.pinpoint.fragments.DroppedPinsFragment;

public class ProfileFragmentAdapter  extends FragmentStateAdapter {

    private Context adapterContext;
    private int tabCount;

    public ProfileFragmentAdapter(@NonNull FragmentManager fragmentManager, int tabs, @NonNull Lifecycle lifecycle){
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
        switch(position){
            case 0:
                ActivityFragment activityFragment = new ActivityFragment();
                return activityFragment;
            case 1:
                DroppedPinsFragment droppedPinsFragment = new DroppedPinsFragment();
                return droppedPinsFragment;
            default:
                return null;
        }
    }

}
