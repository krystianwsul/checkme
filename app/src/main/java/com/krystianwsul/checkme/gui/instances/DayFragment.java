package com.krystianwsul.checkme.gui.instances;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.utils.time.Date;

import junit.framework.Assert;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DayFragment extends Fragment {
    private static final String POSITION_KEY = "position";
    private static final String TIME_RANGE_KEY = "timeRange";

    public static DayFragment newInstance(MainActivity.TimeRange timeRange, int day) {
        Assert.assertTrue(timeRange != null);
        Assert.assertTrue(day >= 0);

        DayFragment dayFragment = new DayFragment();

        Bundle args = new Bundle();
        args.putInt(POSITION_KEY, day);
        args.putSerializable(TIME_RANGE_KEY, timeRange);

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

        Assert.assertTrue(args.containsKey(POSITION_KEY));
        int position = args.getInt(POSITION_KEY);
        Assert.assertTrue(position >= 0);

        Assert.assertTrue(args.containsKey(TIME_RANGE_KEY));
        MainActivity.TimeRange timeRange = (MainActivity.TimeRange) args.getSerializable(TIME_RANGE_KEY);
        Assert.assertTrue(timeRange != null);

        View view = getView();
        Assert.assertTrue(view != null);

        String title;

        if (timeRange == MainActivity.TimeRange.DAY) {
            switch (position) {
                case 0:
                    title = getActivity().getString(R.string.today);
                    break;
                case 1:
                    title = getActivity().getString(R.string.tomorrow);
                    break;
                default:
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DATE, position);
                    Date date = new Date(calendar);
                    title = date.getDayOfWeek().toString() + ", " + date.toString();
            }
        } else {
            if (timeRange == MainActivity.TimeRange.WEEK) {
                Calendar start = Calendar.getInstance();

                if (position > 0) {
                    start.add(Calendar.WEEK_OF_YEAR, position);
                    start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
                }

                Calendar end = Calendar.getInstance();

                end.add(Calendar.WEEK_OF_YEAR, position + 1);
                end.set(Calendar.DAY_OF_WEEK, end.getFirstDayOfWeek());
                end.add(Calendar.DATE, -1);

                java.util.Date startDate = new java.util.Date(start.getTimeInMillis());
                java.util.Date endDate = new java.util.Date(end.getTimeInMillis());

                DateFormat dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);

                title = dateFormat.format(startDate) + " - " + dateFormat.format(endDate);
            } else {
                Assert.assertTrue(timeRange == MainActivity.TimeRange.MONTH);

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MONTH, position);

                int month = calendar.get(Calendar.MONTH);

                title = DateFormatSymbols.getInstance().getMonths()[month];
            }
        }

        TabLayout dayTabLayout = (TabLayout) view.findViewById(R.id.day_tab_layout);
        Assert.assertTrue(dayTabLayout != null);

        dayTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        dayTabLayout.addTab(dayTabLayout.newTab().setText(title));

        FragmentManager fragmentManager = getChildFragmentManager();
        GroupListFragment groupListFragment = (GroupListFragment) fragmentManager.findFragmentById(R.id.day_frame);

        Assert.assertTrue((savedInstanceState == null) == (groupListFragment == null));

        if (groupListFragment == null)
            fragmentManager.beginTransaction().add(R.id.day_frame, GroupListFragment.getGroupInstance(timeRange, position)).commit();
    }

    @Override
    public void onResume() {
        Crashlytics.log("DayFragment.onResume");

        super.onResume();
    }
}
