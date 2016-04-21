package com.example.krystianwsul.organizator.gui.instances;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.krystianwsul.organizator.R;

import junit.framework.Assert;

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

        ViewPager daysPager = (ViewPager) view.findViewById(R.id.days_pager);
        Assert.assertTrue(daysPager != null);

        daysPager.setAdapter(new FragmentStatePagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return DayFragment.newInstance(position);
            }

            @Override
            public int getCount() {
                return Integer.MAX_VALUE;
            }
        });
    }
}
