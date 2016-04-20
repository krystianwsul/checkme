package com.example.krystianwsul.organizator.gui.instances;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.utils.time.Date;

import junit.framework.Assert;

import java.util.Calendar;

public class DaysFragment extends Fragment {

    public static DaysFragment newInstance() {
        return new DaysFragment();
    }

    public DaysFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_days, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);

        TabLayout daysTabLayout = (TabLayout) view.findViewById(R.id.days_tab_layout);
        Assert.assertTrue(daysTabLayout != null);

        daysTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        ViewPager daysPager = (ViewPager) view.findViewById(R.id.days_pager);
        Assert.assertTrue(daysPager != null);

        daysPager.setAdapter(new FragmentStatePagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return GroupListFragment.getGroupInstance(position);
            }

            @Override
            public int getCount() {
                return Integer.MAX_VALUE;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return getActivity().getString(R.string.today);
                    case 1:
                        return getActivity().getString(R.string.tomorrow);
                    default:
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DAY_OF_YEAR, position);
                        return new Date(calendar).toString();
                }
            }
        });

        //daysTabLayout.setupWithViewPager(daysPager);
    }
}
