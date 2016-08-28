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
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
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
import java.util.Map;

public class SingleScheduleDialogFragment extends DialogFragment {
    private static final String DATE_STATE_KEY = "date";
    private static final String TIME_PAIR_PERSIST_STATE_KEY = "timePairPersist";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";
    private static final String TIME_PICKER_TAG = "timePicker";

    private static final String DATE_KEY = "date";
    private static final String TIME_PAIR_PERSIST_KEY = "timePairPersist";

    private Map<Integer, SingleScheduleLoader.CustomTimeData> mCustomTimeDatas;
    private SingleScheduleDialogListener mSingleScheduleDialogListener;

    private TextInputLayout mSingleScheduleDialogDateLayout;
    private TextView mSingleScheduleDialogDate;
    private TextInputLayout mSingleScheduleDialogTimeLayout;
    private TextView mSingleScheduleDialogTime;

    private Date mDate;
    private TimePairPersist mTimePairPersist;

    private BroadcastReceiver mBroadcastReceiver;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mCustomTimeDatas != null);

            mTimePairPersist.setCustomTimeId(customTimeId);
            updateTimeText();
            setValidTime();
        }

        @Override
        public void onOtherSelected() {
            Assert.assertTrue(mCustomTimeDatas != null);

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
        Assert.assertTrue(mCustomTimeDatas != null);

        mTimePairPersist.setHourMinute(new HourMinute(hourOfDay, minute));
        updateTimeText();
        setValidTime();
    };

    private MDButton mButton;

    public static SingleScheduleDialogFragment newInstance(Date date, TimePairPersist timePairPersist) {
        SingleScheduleDialogFragment singleScheduleFragment = new SingleScheduleDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(DATE_KEY, date);
        args.putParcelable(TIME_PAIR_PERSIST_KEY, timePairPersist.copy());

        singleScheduleFragment.setArguments(args);
        return singleScheduleFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                .customView(R.layout.fragment_single_schedule_dialog, false)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onPositive((dialog, which) -> {
                    Assert.assertTrue(mCustomTimeDatas != null);
                    Assert.assertTrue(mSingleScheduleDialogListener != null);
                    Assert.assertTrue(isValidDateTime());

                    mSingleScheduleDialogListener.onSingleScheduleDialogResult(mDate, mTimePairPersist);
                })
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

        mButton = materialDialog.getActionButton(DialogAction.POSITIVE);
        Assert.assertTrue(mButton != null);

        Bundle args = getArguments();
        Assert.assertTrue(args != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(DATE_STATE_KEY));
            Assert.assertTrue(savedInstanceState.containsKey(TIME_PAIR_PERSIST_STATE_KEY));

            mDate = savedInstanceState.getParcelable(DATE_STATE_KEY);
            Assert.assertTrue(mDate != null);

            mTimePairPersist = savedInstanceState.getParcelable(TIME_PAIR_PERSIST_STATE_KEY);
            Assert.assertTrue(mTimePairPersist != null);
        } else {
            Assert.assertTrue(args.containsKey(DATE_KEY));
            mDate = args.getParcelable(DATE_KEY);
            Assert.assertTrue(mDate != null);

            Assert.assertTrue(args.containsKey(TIME_PAIR_PERSIST_KEY));
            mTimePairPersist = args.getParcelable(TIME_PAIR_PERSIST_KEY);
            Assert.assertTrue(mTimePairPersist != null);
        }

        return materialDialog;
    }

    public void initialize(Map<Integer, SingleScheduleLoader.CustomTimeData> customTimeDatas, SingleScheduleDialogListener singleScheduleDialogListener) {
        Assert.assertTrue(customTimeDatas != null);
        Assert.assertTrue(singleScheduleDialogListener != null);

        mCustomTimeDatas = customTimeDatas;
        mSingleScheduleDialogListener = singleScheduleDialogListener;

        if (getActivity() != null)
            initialize();
    }

    private void initialize() {
        mSingleScheduleDialogDateLayout.setVisibility(View.VISIBLE);
        mSingleScheduleDialogTimeLayout.setVisibility(View.VISIBLE);

        updateDateText();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSingleScheduleDialogTime.setOnClickListener(v -> {
            Assert.assertTrue(mCustomTimeDatas != null);
            ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas = new ArrayList<>(Stream.of(mCustomTimeDatas.values())
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

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mCustomTimeDatas != null)
                    setValidTime();
            }
        };

        if (mCustomTimeDatas != null)
            initialize();
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("SingleScheduleDialogFragment.onResume");

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
        Assert.assertTrue(mTimePairPersist != null);

        outState.putParcelable(DATE_STATE_KEY, mDate);
        outState.putParcelable(TIME_PAIR_PERSIST_STATE_KEY, mTimePairPersist);
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
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mDate != null);

        if (mTimePairPersist.getCustomTimeId() != null) {
            SingleScheduleLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            mSingleScheduleDialogTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")");
        } else {
            mSingleScheduleDialogTime.setText(mTimePairPersist.getHourMinute().toString());
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isValidDate() {
        return (mDate.compareTo(Date.today()) >= 0);
    }

    private boolean isValidDateTime() {
        if (mCustomTimeDatas != null) {
            HourMinute hourMinute;
            if (mTimePairPersist.getCustomTimeId() != null) {
                if (!mCustomTimeDatas.containsKey(mTimePairPersist.getCustomTimeId()))
                    return false; //cached data doesn't contain new custom time

                hourMinute = mCustomTimeDatas.get(mTimePairPersist.getCustomTimeId()).HourMinutes.get(mDate.getDayOfWeek());
            } else {
                hourMinute = mTimePairPersist.getHourMinute();
            }

            return (new TimeStamp(mDate, hourMinute).compareTo(TimeStamp.getNow()) > 0);
        } else {
            return false;
        }
    }

    private void setValidTime() {
        if (isValidDateTime()) {
            mButton.setEnabled(true);

            mSingleScheduleDialogDateLayout.setError(null);
            mSingleScheduleDialogTimeLayout.setError(null);
        } else {
            mButton.setEnabled(false);

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);

        Assert.assertTrue(mTimePairPersist != null);

        if (resultCode > 1)
            mTimePairPersist.setCustomTimeId(resultCode);
    }

    public interface SingleScheduleDialogListener {
        void onSingleScheduleDialogResult(Date date, TimePairPersist timePairPersist);
    }
}