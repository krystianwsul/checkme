package com.example.krystianwsul.organizatortest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;

public class SingleScheduleFragment extends Fragment implements DatePickerFragment.DatePickerFragmentListener {
    private TextView mDateView;

    private Date mDate;

    private static String YEAR_KEY = "year";
    private static String MONTH_KEY = "month";
    private static String DAY_KEY = "day";

    private static String SCHEDULE_TIME_FRAGMENT_KEY = "scheduleTimeFragment";

    public static SingleScheduleFragment newInstance() {
        return new SingleScheduleFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_schedule, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int initialCount = 1;
        if (savedInstanceState != null) {

            int year = savedInstanceState.getInt(YEAR_KEY, -1);
            int month = savedInstanceState.getInt(MONTH_KEY, -1);
            int day = savedInstanceState.getInt(DAY_KEY, -1);

            Assert.assertTrue(year != -1);
            Assert.assertTrue(month != -1);
            Assert.assertTrue(day != -1);

            mDate = new Date(year, month, day);

            FragmentManager fragmentManager = getChildFragmentManager();
            Assert.assertTrue(fragmentManager != null);

            Fragment fragment = fragmentManager.getFragment(savedInstanceState, SCHEDULE_TIME_FRAGMENT_KEY);
            Assert.assertTrue(fragment != null);

            fragmentManager.beginTransaction().replace(R.id.single_schedule_time, fragment).commit();
        } else {
            mDate = Date.today();
        }

        View view = getView();
        Assert.assertTrue(view != null);

        mDateView = (TextView) view.findViewById(R.id.single_schedule_date);
        Assert.assertTrue(mDateView != null);

        mDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getChildFragmentManager();
                DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(mDate);
                datePickerFragment.show(fragmentManager, "date");
            }
        });

        updateDateText();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Assert.assertTrue(mDate != null);

        outState.putInt(YEAR_KEY, mDate.getYear());
        outState.putInt(MONTH_KEY, mDate.getMonth());
        outState.putInt(DAY_KEY, mDate.getDay());

        FragmentManager fragmentManager = getChildFragmentManager();

        Fragment fragment = fragmentManager.findFragmentById(R.id.single_schedule_time);
        Assert.assertTrue(fragment != null);

        fragmentManager.putFragment(outState, SCHEDULE_TIME_FRAGMENT_KEY, fragment);
    }

    private void updateDateText() {
        Assert.assertTrue(mDate != null);
        Assert.assertTrue(mDateView != null);

        mDateView.setText(mDate.getDisplayText(getContext()));
    }

    @Override
    public void onDatePickerFragmentResult(Date date) {
        Assert.assertTrue(date != null);

        mDate = date;
        updateDateText();
    }

    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        ScheduleTimeFragment scheduleTimeFragment = (ScheduleTimeFragment) getChildFragmentManager().findFragmentById(R.id.single_schedule_time);
        Assert.assertTrue(scheduleTimeFragment != null);

        scheduleTimeFragment.onHourMinutePickerFragmentResult(hourMinute);
    }
}