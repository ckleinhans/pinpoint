package edu.wisc.ece.pinpoint.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import edu.wisc.ece.pinpoint.ProfileFragmentAdapter;
import edu.wisc.ece.pinpoint.R;

public class ProfilePageFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private BottomNavigationView navBar;
    private FloatingActionButton mapButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tabLayout = getView().findViewById(R.id.tab_layout);
        viewPager = getView().findViewById(R.id.view_pager);
        tabLayout.addTab(tabLayout.newTab().setText("Activity"));
        tabLayout.addTab(tabLayout.newTab().setText("Dropped Pins"));
        ProfileFragmentAdapter fragmentAdapter = new ProfileFragmentAdapter(getActivity().getSupportFragmentManager(),  tabLayout.getTabCount(), getLifecycle());
        viewPager.setAdapter(fragmentAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Mandatory override meethod left intentionally blank because we will not implement onTabUnselected
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Mandatory override meethod left intentionally blank because we will not implement onTabReselected
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.getTabAt(position).select();
            }
        });
    }
}