package com.example.krystianwsul.organizatortest.gui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

public class SingleScheduleFragment extends Fragment implements DatePickerFragment.DatePickerFragmentListener, HourMinutePickerFragment.HourMinutePickerFragmentListener, ScheduleFragment {
    private TextView mDateView;
    private TimePickerView mTimePickerView;

    private Date mDate;

    private static String YEAR_KEY = "year";
    private static String MONTH_KEY = "month";
    private static String DAY_KEY = "day";

    public static SingleScheduleFragment newInstance() {
        return new SingleScheduleFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Assert.assertTrue(context instanceof DatePickerFragment.DatePickerFragmentListener);
        Assert.assertTrue(context instanceof HourMinutePickerFragment.HourMinutePickerFragmentListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_schedule, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {

            int year = savedInstanceState.getInt(YEAR_KEY, -1);
            int month = savedInstanceState.getInt(MONTH_KEY, -1);
            int day = savedInstanceState.getInt(DAY_KEY, -1);

            Assert.assertTrue(year != -1);
            Assert.assertTrue(month != -1);
            Assert.assertTrue(day != -1);

            mDate = new Date(year, month, day);
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
                DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(getActivity(), mDate);
                datePickerFragment.show(fragmentManager, "date");
            }
        });

        updateDateText();

        mTimePickerView = (TimePickerView) view.findViewById(R.id.single_schedule_timepickerview);

        mTimePickerView.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
            @Override
            public void onCustomTimeSelected(CustomTime customTime) {

            }

            @Override
            public void onHourMinuteSelected(HourMinute hourMinute) {

            }

            @Override
            public void onHourMinuteClick() {
                FragmentManager fragmentManager = getChildFragmentManager();
                HourMinutePickerFragment hourMinutePickerFragment = HourMinutePickerFragment.newInstance(getActivity(), mTimePickerView.getHourMinute());
                hourMinutePickerFragment.show(fragmentManager, "time");
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Assert.assertTrue(mDate != null);

        outState.putInt(YEAR_KEY, mDate.getYear());
        outState.putInt(MONTH_KEY, mDate.getMonth());
        outState.putInt(DAY_KEY, mDate.getDay());
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

    @Override
    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        mTimePickerView.setHourMinute(hourMinute);
    }

    @Override
    public boolean isValidTime() {
        return (new TimeStamp(mDate, mTimePickerView.getTime().getHourMinute(mDate.getDayOfWeek())).compareTo(TimeStamp.getNow()) > 0);
    }

    @Override
    public Schedule createSchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        return TaskFactory.getInstance().createSingleSchedule(rootTask, mDate, mTimePickerView.getTime());
    }

    public static class PastTimeException extends Exception {

    }
}