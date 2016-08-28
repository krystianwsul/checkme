package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.MyCalendarFragment;
import com.krystianwsul.checkme.gui.TimeDialogFragment;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.SingleScheduleLoader;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePairPersist;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class SingleScheduleDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<SingleScheduleLoader.Data> {
    private static final String SCHEDULE_HINT_KEY = "scheduleHint";

    private static final String INITIAL_HOUR_MINUTE_KEY = "initialHourMinute";

    private static final String PARCEL_DATE_KEY = "date";
    private static final String TIME_PAIR_PERSIST_KEY = "timePairPersist";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";
    private static final String TIME_PICKER_TAG = "timePicker";

    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private Integer mRootTaskId;
    private SingleScheduleLoader.Data mData;

    private TextInputLayout mSingleScheduleDialogDateLayout;
    private TextView mSingleScheduleDialogDate;
    private TextInputLayout mSingleScheduleDialogTimeLayout;
    private TextView mSingleScheduleDialogTime;

    private Date mDate;
    private TimePairPersist mTimePairPersist;

    private BroadcastReceiver mBroadcastReceiver;

    private Bundle mSavedInstanceState;

    private boolean mFirst = true;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mData != null);

            mTimePairPersist.setCustomTimeId(customTimeId);
            updateTimeText();
            setValidTime();
        }

        @Override
        public void onOtherSelected() {
            Assert.assertTrue(mData != null);

            RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
            radialTimePickerDialogFragment.setStartTime(mTimePairPersist.getHourMinute().getHour(), mTimePairPersist.getHourMinute().getMinute());
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
            radialTimePickerDialogFragment.show(getChildFragmentManager(), TIME_PICKER_TAG);
        }

        @Override
        public void onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(getActivity()), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        }
    };

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = (dialog, hourOfDay, minute) -> {
        Assert.assertTrue(mData != null);

        mTimePairPersist.setHourMinute(new HourMinute(hourOfDay, minute));
        updateTimeText();
        setValidTime();
    };

    private HourMinute mInitialHourMinute;

    public static SingleScheduleDialogFragment newInstance() {
        return new SingleScheduleDialogFragment();
    }

    public static SingleScheduleDialogFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        SingleScheduleDialogFragment singleScheduleFragment = new SingleScheduleDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);
        singleScheduleFragment.setArguments(args);

        return singleScheduleFragment;
    }

    public static SingleScheduleDialogFragment newInstance(int rootTaskId) {
        SingleScheduleDialogFragment singleScheduleFragment = new SingleScheduleDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        singleScheduleFragment.setArguments(args);
        return singleScheduleFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mSavedInstanceState = savedInstanceState;

        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                .customView(R.layout.fragment_single_schedule_dialog, false)
                .build();

        View view = materialDialog.getCustomView();
        Assert.assertTrue(view != null);

        mSingleScheduleDialogDateLayout = (TextInputLayout) view.findViewById(R.id.single_schedule_dialog_date_layout);
        Assert.assertTrue(mSingleScheduleDialogDateLayout != null);

        mSingleScheduleDialogDate = (TextView) view.findViewById(R.id.single_schedule_dialog_date);
        Assert.assertTrue(mSingleScheduleDialogDate != null);

        mSingleScheduleDialogTimeLayout = (TextInputLayout) view.findViewById(R.id.single_schedule_dialog_time_layout);
        Assert.assertTrue(mSingleScheduleDialogTimeLayout != null);

        mSingleScheduleDialogTime = (TextView) view.findViewById(R.id.single_schedule_dialog_time);
        Assert.assertTrue(mSingleScheduleDialogTime != null);

        return materialDialog;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSingleScheduleDialogTime.setOnClickListener(v -> {
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
        mSingleScheduleDialogDate.setOnClickListener(v -> {
            MyCalendarFragment calendarDatePickerDialogFragment = new MyCalendarFragment();
            calendarDatePickerDialogFragment.setDate(mDate);
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
            calendarDatePickerDialogFragment.show(getChildFragmentManager(), DATE_FRAGMENT_TAG);
        });
        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getChildFragmentManager().findFragmentByTag(DATE_FRAGMENT_TAG);
        if (calendarDatePickerDialogFragment != null)
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(!args.containsKey(SCHEDULE_HINT_KEY));

                mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
                Assert.assertTrue(mRootTaskId != -1);
            } else {
                Assert.assertTrue(args.containsKey(SCHEDULE_HINT_KEY));
            }
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(PARCEL_DATE_KEY)) {
            Assert.assertTrue(savedInstanceState.containsKey(TIME_PAIR_PERSIST_KEY));
            Assert.assertTrue(savedInstanceState.containsKey(INITIAL_HOUR_MINUTE_KEY));

            mDate = savedInstanceState.getParcelable(PARCEL_DATE_KEY);
            Assert.assertTrue(mDate != null);

            mTimePairPersist = savedInstanceState.getParcelable(TIME_PAIR_PERSIST_KEY);
            Assert.assertTrue(mTimePairPersist != null);

            mInitialHourMinute = savedInstanceState.getParcelable(INITIAL_HOUR_MINUTE_KEY);
        } else if (args != null) {
            if (!args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(args.containsKey(SCHEDULE_HINT_KEY));

                CreateTaskActivity.ScheduleHint scheduleHint = args.getParcelable(SCHEDULE_HINT_KEY);
                Assert.assertTrue(scheduleHint != null);

                mDate = scheduleHint.mDate;

                if (scheduleHint.mTimePair != null) {
                    mTimePairPersist = new TimePairPersist(scheduleHint.mTimePair);
                } else {
                    mTimePairPersist = new TimePairPersist();
                }
                mInitialHourMinute = mTimePairPersist.getHourMinute();
            }
        } else {
            mDate = Date.today();
            mTimePairPersist = new TimePairPersist();
            mInitialHourMinute = mTimePairPersist.getHourMinute();
        }

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
        Assert.assertTrue(mSingleScheduleDialogDate != null);

        mSingleScheduleDialogDate.setText(mDate.getDisplayText(getContext()));

        updateTimeText();

        setValidTime();
    }

    @SuppressLint("SetTextI18n")
    private void updateTimeText() {
        Assert.assertTrue(mTimePairPersist != null);
        Assert.assertTrue(mSingleScheduleDialogTime != null);
        Assert.assertTrue(mData != null);
        Assert.assertTrue(mDate != null);

        if (mTimePairPersist.getCustomTimeId() != null) {
            SingleScheduleLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            mSingleScheduleDialogTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")");
        } else {
            mSingleScheduleDialogTime.setText(mTimePairPersist.getHourMinute().toString());
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
                if (mTimePairPersist.getCustomTimeId() != null) {
                    if (!mData.CustomTimeDatas.containsKey(mTimePairPersist.getCustomTimeId()))
                        return false; //cached data doesn't contain new custom time

                    hourMinute = mData.CustomTimeDatas.get(mTimePairPersist.getCustomTimeId()).HourMinutes.get(mDate.getDayOfWeek());
                } else {
                    hourMinute = mTimePairPersist.getHourMinute();
                }

                return (new TimeStamp(mDate, hourMinute).compareTo(TimeStamp.getNow()) > 0);
            }
        } else {
            return false;
        }
    }

    private void setValidTime() {
        if (isValidDateTime()) {
            mSingleScheduleDialogDateLayout.setError(null);
            mSingleScheduleDialogTimeLayout.setError(null);
        } else {
            if (isValidDate()) {
                mSingleScheduleDialogDateLayout.setError(null);
                mSingleScheduleDialogTimeLayout.setError(getString(R.string.error_time));
            } else {
                mSingleScheduleDialogDateLayout.setError(getString(R.string.error_date));
                mSingleScheduleDialogTimeLayout.setError(null);
            }
        }
    }

    @Override
    public Loader<SingleScheduleLoader.Data> onCreateLoader(int id, Bundle args) {
        return new SingleScheduleLoader(getActivity(), mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<SingleScheduleLoader.Data> loader, SingleScheduleLoader.Data data) {
        mData = data;

        if (mFirst && mData.ScheduleData != null && (mSavedInstanceState == null || !mSavedInstanceState.containsKey(PARCEL_DATE_KEY))) {
            Assert.assertTrue(mDate == null);
            Assert.assertTrue(mTimePairPersist == null);

            mFirst = false;

            mDate = mData.ScheduleData.Date;
            mTimePairPersist = new TimePairPersist(mData.ScheduleData.TimePair);
        }

        mSingleScheduleDialogDateLayout.setVisibility(View.VISIBLE);
        mSingleScheduleDialogTimeLayout.setVisibility(View.VISIBLE);

        updateDateText();
    }

    @Override
    public void onLoaderReset(Loader<SingleScheduleLoader.Data> loader) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);

        Assert.assertTrue(mTimePairPersist != null);

        if (resultCode > 1)
            mTimePairPersist.setCustomTimeId(resultCode);
    }
}