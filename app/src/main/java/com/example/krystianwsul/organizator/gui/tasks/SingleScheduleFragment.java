package com.example.krystianwsul.organizator.gui.tasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.SingleScheduleLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class SingleScheduleFragment extends Fragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<SingleScheduleLoader.Data> {
    private static final String DATE_KEY = "date";
    private static final String HOUR_MINUTE_KEY = "hourMinute";

    private static final String YEAR_KEY = "year";
    private static final String MONTH_KEY = "month";
    private static final String DAY_KEY = "day";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_PICKER_TAG = "timePicker";

    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private Integer mRootTaskId;
    private SingleScheduleLoader.Data mData;

    private Bundle mSavedInstanceState;

    private TextView mDateView;
    private TimePickerView mTimePickerView;

    private Date mDate;
    private HourMinute mHourMinute;

    private BroadcastReceiver mBroadcastReceiver;

    public static SingleScheduleFragment newInstance() {
        return new SingleScheduleFragment();
    }

    public static SingleScheduleFragment newInstance(Date date) {
        Assert.assertTrue(date != null);

        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(DATE_KEY, date);
        singleScheduleFragment.setArguments(args);

        return singleScheduleFragment;
    }

    public static SingleScheduleFragment newInstance(Date date, HourMinute hourMinute) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(hourMinute != null);

        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(DATE_KEY, date);
        args.putParcelable(HOUR_MINUTE_KEY, hourMinute);
        singleScheduleFragment.setArguments(args);

        return singleScheduleFragment;
    }

    public static SingleScheduleFragment newInstance(int rootTaskId) {
        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        singleScheduleFragment.setArguments(args);
        return singleScheduleFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_schedule, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(!args.containsKey(DATE_KEY));

                mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
                Assert.assertTrue(mRootTaskId != -1);
            } else {
                Assert.assertTrue(args.containsKey(DATE_KEY));

                mDate = args.getParcelable(DATE_KEY);
                Assert.assertTrue(mDate != null);

                if (args.containsKey(HOUR_MINUTE_KEY)) {
                    mHourMinute = args.getParcelable(HOUR_MINUTE_KEY);
                    Assert.assertTrue(mHourMinute != null);
                }
            }
        }

        View view = getView();
        Assert.assertTrue(view != null);

        mTimePickerView = (TimePickerView) view.findViewById(R.id.single_schedule_timepickerview);
        Assert.assertTrue(mTimePickerView != null);

        final RadialTimePickerDialogFragment.OnTimeSetListener onTimeSetListener = (dialog, hourOfDay, minute) -> {
            mTimePickerView.setHourMinute(new HourMinute(hourOfDay, minute));
            setValidTime();
        };
        mTimePickerView.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
            @Override
            public void onCustomTimeSelected(int customTimeId) {
            }

            @Override
            public void onHourMinuteSelected(HourMinute hourMinute) {
            }

            @Override
            public void onHourMinuteClick() {
                RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
                HourMinute startTime = mTimePickerView.getHourMinute();
                radialTimePickerDialogFragment.setStartTime(startTime.getHour(), startTime.getMinute());
                radialTimePickerDialogFragment.setOnTimeSetListener(onTimeSetListener);
                radialTimePickerDialogFragment.show(getChildFragmentManager(), TIME_PICKER_TAG);
            }
        });
        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(onTimeSetListener);

        mDateView = (TextView) view.findViewById(R.id.single_schedule_date);
        Assert.assertTrue(mDateView != null);

        final CalendarDatePickerDialogFragment.OnDateSetListener onDateSetListener = (dialog, year, monthOfYear, dayOfMonth) -> {
            mDate = new Date(year, monthOfYear + 1, dayOfMonth);
            updateDateText();
        };
        mDateView.setOnClickListener(v -> {
            CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = new CalendarDatePickerDialogFragment();
            calendarDatePickerDialogFragment.setDateRange(new MonthAdapter.CalendarDay(Calendar.getInstance()), null);
            calendarDatePickerDialogFragment.setPreselectedDate(mDate.getYear(), mDate.getMonth() - 1, mDate.getDay());
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
            calendarDatePickerDialogFragment.show(getChildFragmentManager(), DATE_FRAGMENT_TAG);
        });
        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getChildFragmentManager().findFragmentByTag(DATE_FRAGMENT_TAG);
        if (calendarDatePickerDialogFragment != null)
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);

        getLoaderManager().initLoader(0, null, this);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setValidTime();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        setValidTime();
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mBroadcastReceiver);
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
        setValidTime();
    }

    private void setValidTime() {
        boolean valid;

        if (mData != null) {
            HourMinute hourMinute = mTimePickerView.getHourMinute();
            Integer customTimeId = mTimePickerView.getCustomTimeId();
            Assert.assertTrue((hourMinute == null) != (customTimeId == null));

            if (hourMinute == null)
                hourMinute = mData.CustomTimeDatas.get(customTimeId).HourMinutes.get(mDate.getDayOfWeek());

            valid = (new TimeStamp(mDate, hourMinute).compareTo(TimeStamp.getNow()) > 0);
        } else {
            valid = false;
        }
        ((CreateRootTaskActivity) getActivity()).setTimeValid(valid);
    }

    @Override
    public void createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(mRootTaskId == null);

        DomainFactory.getDomainFactory(getActivity()).createSingleScheduleRootTask(mData.DataId, name, mDate, mTimePickerView.getTimePair());

        TickService.startService(getActivity());
    }

    @Override
    public void updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        DomainFactory.getDomainFactory(getActivity()).updateSingleScheduleRootTask(mData.DataId, rootTaskId, name, mDate, mTimePickerView.getTimePair());

        TickService.startService(getActivity());
    }

    @Override
    public void createRootJoinTask(String name, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Assert.assertTrue(mRootTaskId == null);

        DomainFactory.getDomainFactory(getActivity()).createSingleScheduleJoinRootTask(mData.DataId, name, mDate, mTimePickerView.getTimePair(), joinTaskIds);

        TickService.startService(getActivity());
    }

    @Override
    public Loader<SingleScheduleLoader.Data> onCreateLoader(int id, Bundle args) {
        return new SingleScheduleLoader(getActivity(), mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<SingleScheduleLoader.Data> loader, SingleScheduleLoader.Data data) {
        mData = data;

        Bundle args = getArguments();

        if (mSavedInstanceState != null) {
            int year = mSavedInstanceState.getInt(YEAR_KEY, -1);
            int month = mSavedInstanceState.getInt(MONTH_KEY, -1);
            int day = mSavedInstanceState.getInt(DAY_KEY, -1);

            Assert.assertTrue(year != -1);
            Assert.assertTrue(month != -1);
            Assert.assertTrue(day != -1);

            mDate = new Date(year, month, day);
        } else if (args != null) {
            if (args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(!args.containsKey(DATE_KEY));
                Assert.assertTrue(mData.ScheduleData != null);

                mDate = mData.ScheduleData.Date;
            } else {
                Assert.assertTrue(args.containsKey(DATE_KEY));
                Assert.assertTrue(mDate != null);
            }
        } else {
            mDate = Date.today();
        }

        HashMap<Integer, TimePickerView.CustomTimeData> customTimeDatas = new HashMap<>();
        for (SingleScheduleLoader.CustomTimeData customTimeData : mData.CustomTimeDatas.values())
            customTimeDatas.put(customTimeData.Id, new TimePickerView.CustomTimeData(customTimeData.Id, customTimeData.Name));
        mTimePickerView.setCustomTimeDatas(customTimeDatas);

        if (mSavedInstanceState == null && args != null) {
            if (args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(!args.containsKey(DATE_KEY));
                Assert.assertTrue(mData.ScheduleData != null);

                mTimePickerView.setTimePair(mData.ScheduleData.TimePair);
            } else {
                Assert.assertTrue(args.containsKey(DATE_KEY));
                Assert.assertTrue(mData.ScheduleData == null);

                if (args.containsKey(HOUR_MINUTE_KEY)) {
                    Assert.assertTrue(mHourMinute != null);
                    mTimePickerView.setHourMinute(mHourMinute);
                } else {
                    Assert.assertTrue(mHourMinute == null);
                }
            }
        }

        updateDateText();
    }

    @Override
    public void onLoaderReset(Loader<SingleScheduleLoader.Data> loader) {}
}