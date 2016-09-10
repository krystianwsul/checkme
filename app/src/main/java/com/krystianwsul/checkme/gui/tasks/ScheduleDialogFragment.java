package com.krystianwsul.checkme.gui.tasks;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Map;

public abstract class ScheduleDialogFragment extends DialogFragment {
    private static final String DATE_STATE_KEY = "date";
    private static final String DAY_OF_WEEK_STATE_KEY = "dayOfWeek";
    private static final String TIME_PAIR_PERSIST_STATE_KEY = "timePairPersist";

    protected static final String DATE_KEY = "date";
    protected static final String DAY_OF_WEEK_KEY = "dayOfWeek";
    protected static final String TIME_PAIR_PERSIST_KEY = "timePairPersist";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";
    private static final String TIME_PICKER_TAG = "timePicker";

    protected TextInputLayout mScheduleDialogDateLayout;
    protected TextView mScheduleDialogDate;

    protected Spinner mScheduleDialogDay;

    protected TextInputLayout mScheduleDialogTimeLayout;
    protected TextView mScheduleDialogTime;

    protected MDButton mButton;

    protected Map<Integer, ScheduleLoader.CustomTimeData> mCustomTimeDatas;

    protected Date mDate;
    protected DayOfWeek mDayOfWeek;
    protected TimePairPersist mTimePairPersist;

    private BroadcastReceiver mBroadcastReceiver;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mCustomTimeDatas != null);

            mTimePairPersist.setCustomTimeId(customTimeId);

            updateFields();
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
        updateFields();
    };

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                .customView(R.layout.fragment_schedule_dialog, false)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onPositive((dialog, which) -> onPositive())
                .build();

        View view = materialDialog.getCustomView();
        Assert.assertTrue(view != null);

        mScheduleDialogDateLayout = (TextInputLayout) view.findViewById(R.id.schedule_dialog_date_layout);
        Assert.assertTrue(mScheduleDialogDateLayout != null);

        mScheduleDialogDate = (TextView) view.findViewById(R.id.schedule_dialog_date);
        Assert.assertTrue(mScheduleDialogDate != null);

        mScheduleDialogDay = (Spinner) view.findViewById(R.id.schedule_dialog_day);
        Assert.assertTrue(mScheduleDialogDay != null);

        mScheduleDialogTimeLayout = (TextInputLayout) view.findViewById(R.id.schedule_dialog_time_layout);
        Assert.assertTrue(mScheduleDialogTimeLayout != null);

        mScheduleDialogTime = (TextView) view.findViewById(R.id.schedule_dialog_time);
        Assert.assertTrue(mScheduleDialogTime != null);

        mButton = materialDialog.getActionButton(DialogAction.POSITIVE);
        Assert.assertTrue(mButton != null);

        return materialDialog;
    }

    protected abstract void onPositive();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        Assert.assertTrue(args != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(DATE_STATE_KEY));
            Assert.assertTrue(savedInstanceState.containsKey(DAY_OF_WEEK_STATE_KEY));
            Assert.assertTrue(savedInstanceState.containsKey(TIME_PAIR_PERSIST_STATE_KEY));

            mDate = savedInstanceState.getParcelable(DATE_STATE_KEY);
            mDayOfWeek = (DayOfWeek) savedInstanceState.getSerializable(DAY_OF_WEEK_STATE_KEY);
            mTimePairPersist = savedInstanceState.getParcelable(TIME_PAIR_PERSIST_STATE_KEY);
        } else {
            Assert.assertTrue(args.containsKey(DATE_KEY));
            Assert.assertTrue(args.containsKey(DAY_OF_WEEK_KEY));
            Assert.assertTrue(args.containsKey(TIME_PAIR_PERSIST_KEY));

            mDate = args.getParcelable(DATE_KEY);
            mDayOfWeek = (DayOfWeek) args.getSerializable(DAY_OF_WEEK_KEY);
            mTimePairPersist = args.getParcelable(TIME_PAIR_PERSIST_KEY);
        }

        switch (getScheduleType()) {
            case SINGLE:
                Assert.assertTrue(mDate != null);
                Assert.assertTrue(mTimePairPersist != null);

                break;
            case DAILY:
                Assert.assertTrue(mTimePairPersist != null);

                break;
            case WEEKLY:
                Assert.assertTrue(mDayOfWeek != null);
                Assert.assertTrue(mTimePairPersist != null);

                break;
            default:
                throw new UnsupportedOperationException();
        }

        mScheduleDialogTime.setOnClickListener(v -> {
            Assert.assertTrue(mCustomTimeDatas != null);

            ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas;

            switch (getScheduleType()) {
                case SINGLE:
                    Assert.assertTrue(mDate != null);

                    customTimeDatas = Stream.of(mCustomTimeDatas.values())
                            .sortBy(customTimeData -> customTimeData.HourMinutes.get(mDate.getDayOfWeek()))
                            .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")"))
                            .collect(Collectors.toCollection(ArrayList::new));
                    break;
                case DAILY:
                    customTimeDatas = Stream.of(mCustomTimeDatas.values())
                            .sortBy(customTimeData -> customTimeData.Id)
                            .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name))
                            .collect(Collectors.toCollection(ArrayList::new));
                    break;
                case WEEKLY:
                    Assert.assertTrue(mDayOfWeek != null);

                    customTimeDatas = Stream.of(mCustomTimeDatas.values())
                            .sortBy(customTimeData -> customTimeData.HourMinutes.get(mDayOfWeek))
                            .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")"))
                            .collect(Collectors.toCollection(ArrayList::new));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

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
            Assert.assertTrue(getScheduleType() == ScheduleType.SINGLE);

            mDate = new Date(year, monthOfYear + 1, dayOfMonth);
            updateFields();
        };

        mScheduleDialogDate.setOnClickListener(v -> {
            Assert.assertTrue(getScheduleType() == ScheduleType.SINGLE);
            Assert.assertTrue(mDate != null);

            MyCalendarFragment calendarDatePickerDialogFragment = new MyCalendarFragment();
            calendarDatePickerDialogFragment.setDate(mDate);
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
            calendarDatePickerDialogFragment.show(getChildFragmentManager(), DATE_FRAGMENT_TAG);
        });

        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getChildFragmentManager().findFragmentByTag(DATE_FRAGMENT_TAG);
        if (calendarDatePickerDialogFragment != null) {
            Assert.assertTrue(getScheduleType() == ScheduleType.SINGLE);

            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mCustomTimeDatas != null)
                    updateFields();
            }
        };

        ArrayAdapter<DayOfWeek> dayOfWeekAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_no_padding, DayOfWeek.values());
        dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mScheduleDialogDay.setAdapter(dayOfWeekAdapter);
        mScheduleDialogDay.setSelection(dayOfWeekAdapter.getPosition(mDayOfWeek));

        mScheduleDialogDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(getScheduleType() == ScheduleType.WEEKLY);

                DayOfWeek dayOfWeek = dayOfWeekAdapter.getItem(position);
                Assert.assertTrue(dayOfWeek != null);

                mDayOfWeek = dayOfWeek;

                updateFields();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (mCustomTimeDatas != null)
            initialize();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);

        if (resultCode > 1)
            mTimePairPersist.setCustomTimeId(resultCode);
    }

    protected void initialize(@NonNull Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas) {
        mCustomTimeDatas = customTimeDatas;
    }

    protected abstract void initialize();

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        switch (getScheduleType()) {
            case SINGLE:
                Assert.assertTrue(mDate != null);
                Assert.assertTrue(mTimePairPersist != null);

                break;
            case DAILY:
                Assert.assertTrue(mTimePairPersist != null);

                break;
            case WEEKLY:
                Assert.assertTrue(mDayOfWeek != null);
                Assert.assertTrue(mTimePairPersist != null);

                break;
            default:
                throw new UnsupportedOperationException();
        }

        outState.putParcelable(DATE_STATE_KEY, mDate);
        outState.putSerializable(DAY_OF_WEEK_STATE_KEY, mDayOfWeek);
        outState.putParcelable(TIME_PAIR_PERSIST_STATE_KEY, mTimePairPersist);
    }

    @NonNull
    protected abstract ScheduleType getScheduleType();

    protected abstract void updateFields();

    @Override
    public void onResume() {
        super.onResume();

        MyCrashlytics.log("ScheduleDialogFragment.onResume");

        getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        updateFields();
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mBroadcastReceiver);
    }
}
