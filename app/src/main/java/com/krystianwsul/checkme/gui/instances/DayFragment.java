package com.krystianwsul.checkme.gui.instances;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.utils.time.Date;

import junit.framework.Assert;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DayFragment extends AbstractFragment {
    private static final String POSITION_KEY = "position";
    private static final String TIME_RANGE_KEY = "timeRange";

    private TabLayout mDayTabLayout;
    private GroupListFragment mGroupListFragment;

    @NonNull
    public static DayFragment newInstance(@NonNull MainActivity.TimeRange timeRange, int day) {
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
        View view = inflater.inflate(R.layout.fragment_day, container, false);
        Assert.assertTrue(view != null);

        mDayTabLayout = (TabLayout) view.findViewById(R.id.day_tab_layout);
        Assert.assertTrue(mDayTabLayout != null);

        return view;
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

        mDayTabLayout.addTab(mDayTabLayout.newTab().setText(title));

        FragmentManager fragmentManager = getChildFragmentManager();
        mGroupListFragment = (GroupListFragment) fragmentManager.findFragmentById(R.id.day_frame);
        Assert.assertTrue(mGroupListFragment != null);

        mGroupListFragment.setAll(timeRange, position);
    }

    public void selectAll() {
        mGroupListFragment.selectAll();
    }
}
