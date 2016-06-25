package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.MyCalendarFragment;
import com.krystianwsul.checkme.gui.TimeDialogFragment;
import com.krystianwsul.checkme.loaders.SingleScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePairPersist;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class SingleScheduleFragment extends Fragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<SingleScheduleLoader.Data> {
    private static final String ARGUMENT_DATE_KEY = "date";
    private static final String HOUR_MINUTE_KEY = "hourMinute";
    private static final String INITIAL_HOUR_MINUTE_KEY = "initialHourMinute";

    private static final String PARCEL_DATE_KEY = "date";
    private static final String TIME_PAIR_PERSIST_KEY = "timePairPersist";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";
    private static final String TIME_PICKER_TAG = "timePicker";

    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private Integer mRootTaskId;
    private SingleScheduleLoader.Data mData;

    private Bundle mSavedInstanceState;

    private TextInputLayout mSingleScheduleDateLayout;
    private TextView mSingleScheduleDate;
    private TextInputLayout mSingleScheduleTimeLayout;
    private TextView mSingleScheduleTime;

    private Date mDate;
    private TimePairPersist mTimePairPersist;

    private BroadcastReceiver mBroadcastReceiver;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mData != null);

            mTimePairPersist.setCustomTimeId(customTimeId);
            updateTimeText();
            setValidTime();
        }

        @Override
        public void onHourMinuteSelected() {
            Assert.assertTrue(mData != null);

            RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
            radialTimePickerDialogFragment.setStartTime(mTimePairPersist.getHourMinute().getHour(), mTimePairPersist.getHourMinute().getMinute());
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
            radialTimePickerDialogFragment.show(getChildFragmentManager(), TIME_PICKER_TAG);
        }
    };

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = (dialog, hourOfDay, minute) -> {
        Assert.assertTrue(mData != null);

        mTimePairPersist.setHourMinute(new HourMinute(hourOfDay, minute));
        updateTimeText();
        setValidTime();
    };

    private HourMinute mInitialHourMinute;

    public static SingleScheduleFragment newInstance() {
        return new SingleScheduleFragment();
    }

    public static SingleScheduleFragment newInstance(Date date) {
        Assert.assertTrue(date != null);

        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_DATE_KEY, date);
        singleScheduleFragment.setArguments(args);

        return singleScheduleFragment;
    }

    public static SingleScheduleFragment newInstance(Date date, HourMinute hourMinute) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(hourMinute != null);

        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_DATE_KEY, date);
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
                Assert.assertTrue(!args.containsKey(ARGUMENT_DATE_KEY));

                mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
                Assert.assertTrue(mRootTaskId != -1);
            } else {
                Assert.assertTrue(args.containsKey(ARGUMENT_DATE_KEY));
            }
        }

        View view = getView();
        Assert.assertTrue(view != null);

        mSingleScheduleDateLayout = (TextInputLayout) view.findViewById(R.id.single_schedule_date_layout);
        Assert.assertTrue(mSingleScheduleDateLayout != null);

        mSingleScheduleDate = (TextView) view.findViewById(R.id.single_schedule_date);
        Assert.assertTrue(mSingleScheduleDate != null);

        mSingleScheduleTimeLayout = (TextInputLayout) view.findViewById(R.id.single_schedule_time_layout);
        Assert.assertTrue(mSingleScheduleTimeLayout != null);

        mSingleScheduleTime = (TextView) view.findViewById(R.id.single_schedule_time);
        Assert.assertTrue(mSingleScheduleTime != null);

        mSingleScheduleTime.setOnClickListener(v -> {
            Assert.assertTrue(mData != null);
            ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas = new ArrayList<>(Stream.of(mData.CustomTimeDatas.values())
                    .sortBy(customTimeData -> customTimeData.HourMinutes.get(mDate.getDayOfWeek()))
                    .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")"))
                    .collect(Collectors.toList()));

            TimeDialogFragment timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas);
            Assert.assertTrue(timeDialogFragment != null);

            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

            timeDialogFragment.show(getChildFragmentManager(), TIME_LIST_FRAGMENT_TAG);
        });

        TimeDialogFragment timeDialogFragment = (TimeDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_LIST_FRAGMENT_TAG);
        if (timeDialogFragment != null)
            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);

        final CalendarDatePickerDialogFragment.OnDateSetListener onDateSetListener = (dialog, year, monthOfYear, dayOfMonth) -> {
            mDate = new Date(year, monthOfYear + 1, dayOfMonth);
            updateDateText();
        };
        mSingleScheduleDate.setOnClickListener(v -> {
            MyCalendarFragment calendarDatePickerDialogFragment = new MyCalendarFragment();
            calendarDatePickerDialogFragment.setDate(mDate);
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
                if (mData != null)
                    setValidTime();
            }
        };
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("SingleScheduleFragment.onResume");

        super.onResume();

        getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        if (mData != null)
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

        if (mData != null) {
            Assert.assertTrue(mDate != null);
            Assert.assertTrue(mTimePairPersist != null);

            outState.putParcelable(PARCEL_DATE_KEY, mDate);
            outState.putParcelable(TIME_PAIR_PERSIST_KEY, mTimePairPersist);
            outState.putParcelable(INITIAL_HOUR_MINUTE_KEY, mInitialHourMinute);
        }
    }

    private void updateDateText() {
        Assert.assertTrue(mDate != null);
        Assert.assertTrue(mSingleScheduleDate != null);

        mSingleScheduleDate.setText(mDate.getDisplayText(getContext()));

        updateTimeText();

        setValidTime();
    }

    @SuppressLint("SetTextI18n")
    private void updateTimeText() {
        Assert.assertTrue(mTimePairPersist != null);
        Assert.assertTrue(mSingleScheduleTime != null);
        Assert.assertTrue(mData != null);
        Assert.assertTrue(mDate != null);

        if (mTimePairPersist.getCustomTimeId() != null) {
            SingleScheduleLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            mSingleScheduleTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")");
        } else {
            mSingleScheduleTime.setText(mTimePairPersist.getHourMinute().toString());
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isValidDate() {
        if (mData != null) {
            return (mDate.compareTo(Date.today()) >= 0);
        } else {
            return false;
        }
    }

    private boolean isValidDateTime() {
        if (mData != null) {
            if (mData.ScheduleData != null && mData.ScheduleData.TimePair.equals(mTimePairPersist.getTimePair())) {
                return true;
            } else {
                HourMinute hourMinute;
                if (mTimePairPersist.getCustomTimeId() != null)
                    hourMinute = mData.CustomTimeDatas.get(mTimePairPersist.getCustomTimeId()).HourMinutes.get(mDate.getDayOfWeek());
                else
                    hourMinute = mTimePairPersist.getHourMinute();

                return (new TimeStamp(mDate, hourMinute).compareTo(TimeStamp.getNow()) > 0);
            }
        } else {
            return false;
        }
    }

    private void setValidTime() {
        if (isValidDateTime()) {
            ((CreateRootTaskActivity) getActivity()).setTimeValid(true);

            mSingleScheduleDateLayout.setError(null);
            mSingleScheduleTimeLayout.setError(null);
        } else {
            ((CreateRootTaskActivity) getActivity()).setTimeValid(false);

            if (isValidDate()) {
                mSingleScheduleDateLayout.setError(null);
                mSingleScheduleTimeLayout.setError(getString(R.string.error_time));
            } else {
                mSingleScheduleDateLayout.setError(getString(R.string.error_date));
                mSingleScheduleTimeLayout.setError(null);
            }
        }
    }

    @Override
    public void createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(mRootTaskId == null);

        DomainFactory.getDomainFactory(getActivity()).createSingleScheduleRootTask(mData.DataId, name, mDate, mTimePairPersist.getTimePair());

        TickService.startService(getActivity());
    }

    @Override
    public void updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        DomainFactory.getDomainFactory(getActivity()).updateSingleScheduleRootTask(mData.DataId, rootTaskId, name, mDate, mTimePairPersist.getTimePair());

        TickService.startService(getActivity());
    }

    @Override
    public void createRootJoinTask(String name, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Assert.assertTrue(mRootTaskId == null);

        DomainFactory.getDomainFactory(getActivity()).createSingleScheduleJoinRootTask(mData.DataId, name, mDate, mTimePairPersist.getTimePair(), joinTaskIds);

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

        mSingleScheduleDate.setVisibility(View.VISIBLE);
        mSingleScheduleTimeLayout.setVisibility(View.VISIBLE);

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(PARCEL_DATE_KEY)) {
            Assert.assertTrue(mSavedInstanceState.containsKey(TIME_PAIR_PERSIST_KEY));
            Assert.assertTrue(mSavedInstanceState.containsKey(INITIAL_HOUR_MINUTE_KEY));

            mDate = mSavedInstanceState.getParcelable(PARCEL_DATE_KEY);
            Assert.assertTrue(mDate != null);

            mTimePairPersist = mSavedInstanceState.getParcelable(TIME_PAIR_PERSIST_KEY);
            Assert.assertTrue(mTimePairPersist != null);

            mInitialHourMinute = mSavedInstanceState.getParcelable(INITIAL_HOUR_MINUTE_KEY);
        } else if (args != null) {
            if (args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(!args.containsKey(ARGUMENT_DATE_KEY));
                Assert.assertTrue(mData.ScheduleData != null);

                mDate = mData.ScheduleData.Date;
                mTimePairPersist = new TimePairPersist(mData.ScheduleData.TimePair);
            } else {
                Assert.assertTrue(args.containsKey(ARGUMENT_DATE_KEY));
                mDate = args.getParcelable(ARGUMENT_DATE_KEY);
                Assert.assertTrue(mDate != null);

                if (args.containsKey(HOUR_MINUTE_KEY)) {
                    HourMinute hourMinute = args.getParcelable(HOUR_MINUTE_KEY);
                    Assert.assertTrue(hourMinute != null);

                    mTimePairPersist = new TimePairPersist(hourMinute);
                } else {
                    mTimePairPersist = new TimePairPersist();
                }
                mInitialHourMinute = mTimePairPersist.getHourMinute();
            }
        } else {
            mDate = Date.today();
            mTimePairPersist = new TimePairPersist();
        }

        updateDateText();
    }

    @Override
    public void onLoaderReset(Loader<SingleScheduleLoader.Data> loader) {}

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean dataChanged() {
        if (mRootTaskId == null) {
            if (mData == null)
                return false;

            Assert.assertTrue(mData.ScheduleData == null);

            Bundle args = getArguments();

            Date initialDate;
            if (args != null && args.containsKey(ARGUMENT_DATE_KEY)) {
                initialDate = args.getParcelable(ARGUMENT_DATE_KEY);
                Assert.assertTrue(initialDate != null);
            } else {
                initialDate = Date.today();
            }

            if (!mDate.equals(initialDate))
                return true;

            if (mTimePairPersist.getCustomTimeId() != null)
                return true;

            Assert.assertTrue(mInitialHourMinute != null);

            if (!mInitialHourMinute.equals(mTimePairPersist.getHourMinute()))
                return true;

            return false;
        } else {
            if (mData == null)
                return false;

            Assert.assertTrue(mData.ScheduleData != null);

            if (!mData.ScheduleData.Date.equals(mDate))
                return true;

            if (!mData.ScheduleData.TimePair.equals(mTimePairPersist.getTimePair()))
                return true;

            return false;
        }
    }
}