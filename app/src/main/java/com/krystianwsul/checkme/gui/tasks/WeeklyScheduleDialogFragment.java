package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.TimeDialogFragment;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Map;

public class WeeklyScheduleDialogFragment extends DialogFragment {
    private static final String TIME_PICKER_TAG = "timePicker";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";

    private static final String DAY_OF_WEEK_KEY = "dayOfWeek";
    private static final String TIME_PAIR_PERSIST_KEY = "timePairPersist";

    private static final String DAY_OF_WEEK_STATE_KEY = "dayOfWeek";
    private static final String TIME_PAIR_PERSIST_STATE_KEY = "timePairPersist";

    private Map<Integer, ScheduleLoader.CustomTimeData> mCustomTimeDatas;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mCustomTimeDatas != null);

            mTimePairPersist.setCustomTimeId(customTimeId);

            updateTime();
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

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = new RadialTimePickerDialogFragment.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
            Assert.assertTrue(mCustomTimeDatas != null);

            mTimePairPersist.setHourMinute(new HourMinute(hourOfDay, minute));

            updateTime();
        }
    };

    private Spinner mWeeklyScheduleDialogDay;

    private TextView mWeeklyScheduleDialogTime;

    private DayOfWeek mDayOfWeek;
    private TimePairPersist mTimePairPersist;

    private WeeklyScheduleDialogListener mWeeklyScheduleDialogListener;

    public static WeeklyScheduleDialogFragment newInstance(DayOfWeek dayOfWeek, TimePairPersist timePairPersist) {
        Assert.assertTrue(dayOfWeek != null);
        Assert.assertTrue(timePairPersist != null);

        WeeklyScheduleDialogFragment weeklyScheduleFragment = new WeeklyScheduleDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable(DAY_OF_WEEK_KEY, dayOfWeek);
        args.putParcelable(TIME_PAIR_PERSIST_KEY, timePairPersist.copy());
        weeklyScheduleFragment.setArguments(args);

        return weeklyScheduleFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                .customView(R.layout.row_weekly_schedule_dialog, false)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onPositive((dialog, which) -> {
                    Assert.assertTrue(mCustomTimeDatas != null);
                    Assert.assertTrue(mWeeklyScheduleDialogListener != null);

                    mWeeklyScheduleDialogListener.onWeeklyScheduleDialogResult(mDayOfWeek, mTimePairPersist);
                })
                .build();

        View weeklyScheduleRow = materialDialog.getCustomView();
        Assert.assertTrue(weeklyScheduleRow != null);

        mWeeklyScheduleDialogDay = (Spinner) weeklyScheduleRow.findViewById(R.id.weekly_schedule_dialog_day);
        Assert.assertTrue(mWeeklyScheduleDialogDay != null);

        mWeeklyScheduleDialogTime = (TextView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_dialog_time);
        Assert.assertTrue(mWeeklyScheduleDialogTime != null);

        return materialDialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        Assert.assertTrue(args != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(DAY_OF_WEEK_STATE_KEY));
            Assert.assertTrue(savedInstanceState.containsKey(TIME_PAIR_PERSIST_STATE_KEY));

            mDayOfWeek = (DayOfWeek) savedInstanceState.getSerializable(DAY_OF_WEEK_STATE_KEY);
            Assert.assertTrue(mDayOfWeek != null);

            mTimePairPersist = savedInstanceState.getParcelable(TIME_PAIR_PERSIST_STATE_KEY);
            Assert.assertTrue(mTimePairPersist != null);
        } else {
            Assert.assertTrue(args.containsKey(DAY_OF_WEEK_KEY));
            mDayOfWeek = (DayOfWeek) args.getSerializable(DAY_OF_WEEK_KEY);
            Assert.assertTrue(mDayOfWeek != null);

            Assert.assertTrue(args.containsKey(TIME_PAIR_PERSIST_KEY));
            mTimePairPersist = args.getParcelable(TIME_PAIR_PERSIST_KEY);
            Assert.assertTrue(mTimePairPersist != null);
        }

        final ArrayAdapter<DayOfWeek> dayOfWeekAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_no_padding, DayOfWeek.values());
        dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mWeeklyScheduleDialogDay.setAdapter(dayOfWeekAdapter);
        mWeeklyScheduleDialogDay.setSelection(dayOfWeekAdapter.getPosition(mDayOfWeek));

        mWeeklyScheduleDialogDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DayOfWeek dayOfWeek = dayOfWeekAdapter.getItem(position);
                Assert.assertTrue(dayOfWeek != null);

                mDayOfWeek = dayOfWeek;

                updateTime();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mWeeklyScheduleDialogTime.setOnClickListener(v -> {
            Assert.assertTrue(mCustomTimeDatas != null);

            ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas = new ArrayList<>(Stream.of(mCustomTimeDatas.values())
                    .sortBy(customTimeData -> customTimeData.HourMinutes.get(mDayOfWeek))
                    .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")"))
                    .collect(Collectors.toList()));

            TimeDialogFragment timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas);
            Assert.assertTrue(timeDialogFragment != null);

            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

            timeDialogFragment.show(getChildFragmentManager(), TIME_LIST_FRAGMENT_TAG);
        });

        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);

        TimeDialogFragment timeDialogFragment = (TimeDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_LIST_FRAGMENT_TAG);
        if (timeDialogFragment != null)
            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

        if (mCustomTimeDatas != null)
            initialize();
    }

    @SuppressLint("SetTextI18n")
    private void updateTime() {
        if (mTimePairPersist.getCustomTimeId() != null) {
            ScheduleLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            mWeeklyScheduleDialogTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")");
        } else {
            mWeeklyScheduleDialogTime.setText(mTimePairPersist.getHourMinute().toString());
        }
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("WeeklyScheduleDialogFragment.onResume");

        super.onResume();
    }

    public void initialize(Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas, WeeklyScheduleDialogListener weeklyScheduleDialogListener) {
        Assert.assertTrue(customTimeDatas != null);
        Assert.assertTrue(weeklyScheduleDialogListener != null);

        mCustomTimeDatas = customTimeDatas;
        mWeeklyScheduleDialogListener = weeklyScheduleDialogListener;

        if (getActivity() != null)
            initialize();
    }

    private void initialize() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mWeeklyScheduleDialogListener != null);
        Assert.assertTrue(getActivity() != null);

        updateTime();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Assert.assertTrue(mDayOfWeek != null);
        Assert.assertTrue(mTimePairPersist != null);

        outState.putSerializable(DAY_OF_WEEK_STATE_KEY, mDayOfWeek);
        outState.putParcelable(TIME_PAIR_PERSIST_STATE_KEY, mTimePairPersist);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);

        if (resultCode > 0)
            mTimePairPersist.setCustomTimeId(resultCode);
    }

    public interface WeeklyScheduleDialogListener {
        void onWeeklyScheduleDialogResult(DayOfWeek dayOfWeek, TimePairPersist timePairPersist);
    }
}
