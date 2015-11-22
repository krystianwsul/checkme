package com.example.krystianwsul.organizatortest;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.security.Timestamp;

public class SingleScheduleFragment extends Fragment implements DatePickerFragment.DatePickerFragmentListener, TimePickerFragment.TimePickerFragmentListener {
    private TextView mDateView;
    private TextView mTimeView;

    private Date mDate;
    private Time mTime;

    public static SingleScheduleFragment newInstance() {
        SingleScheduleFragment fragment = new SingleScheduleFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_schedule, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mDateView = (TextView) getView().findViewById(R.id.single_schedule_date);
        mTimeView = (TextView) getView().findViewById(R.id.single_schedule_time);

        mDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getChildFragmentManager();
                DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(mDate);
                datePickerFragment.show(fragmentManager, "date");
            }
        });

        mTimeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getChildFragmentManager();
                TimePickerFragment timePickerFragment = TimePickerFragment.newInstance(mTime.getHourMinute(mDate.getDayOfWeek()));
                timePickerFragment.show(fragmentManager, "time");
            }
        });

        TimeStamp now = TimeStamp.getNow();
        mDate = now.getDate();
        mTime = new NormalTime(now.getHourMinute());

        updateDateText();
        updateTimeText();
    }

    private void updateDateText() {
        Assert.assertTrue(mDate != null);
        Assert.assertTrue(mDateView != null);

        mDateView.setText(mDate.getDisplayText(getContext()));
    }

    private void updateTimeText() {
        Assert.assertTrue(mTime != null);
        Assert.assertTrue(mTimeView != null);

        mTimeView.setText(mTime.toString());
    }

    @Override
    public void onDatePickerFragmentResult(Date date) {
        Assert.assertTrue(date != null);

        mDate = date;
        updateDateText();
    }

    @Override
    public void onTimePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        mTime = new NormalTime(hourMinute);
        updateTimeText();
    }
}