package com.example.krystianwsul.organizator.gui.instances;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.utils.time.Date;

import junit.framework.Assert;

import java.util.Calendar;

public class DayFragment extends Fragment {
    private static final String DAY_KEY = "day";

    public static DayFragment newInstance(int day) {
        Assert.assertTrue(day >= 0);

        DayFragment dayFragment = new DayFragment();

        Bundle args = new Bundle();
        args.putInt(DAY_KEY, day);

        dayFragment.setArguments(args);
        return dayFragment;
    }

    public DayFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_day, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        Assert.assertTrue(args != null);

        Assert.assertTrue(args.containsKey(DAY_KEY));
        int day = args.getInt(DAY_KEY);
        Assert.assertTrue(day >= 0);

        View view = getView();
        Assert.assertTrue(view != null);

        String title;
        switch (day) {
            case 0:
                title = getActivity().getString(R.string.today);
                break;
            case 1:
                title = getActivity().getString(R.string.tomorrow);
                break;
            default:
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, day);
                title = new Date(calendar).toString();
        }

        TabLayout dayTabLayout = (TabLayout) view.findViewById(R.id.day_tab_layout);
        Assert.assertTrue(dayTabLayout != null);

        dayTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        dayTabLayout.addTab(dayTabLayout.newTab().setText(title));

        getChildFragmentManager().beginTransaction().add(R.id.day_frame, GroupListFragment.getGroupInstance(day)).commit();
    }
}
