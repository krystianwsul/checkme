package com.krystianwsul.checkme.gui.tasks;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
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
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Map;

public class DailyScheduleDialogFragment extends ScheduleDialogFragment {
    private static final String TIME_PICKER_TAG = "timePicker";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";

    private static final String TIME_PAIR_PERSIST_KEY = "timePairPersist";

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

    private DailyScheduleDialogListener mDailyScheduleDialogListener;

    private TimePairPersist mTimePairPersist;

    private TextView mDailyScheduleDialogTime;

    public static DailyScheduleDialogFragment newInstance(TimePairPersist timePairPersist) {
        Assert.assertTrue(timePairPersist != null);

        DailyScheduleDialogFragment dailyScheduleFragment = new DailyScheduleDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(TIME_PAIR_PERSIST_KEY, timePairPersist.copy());
        dailyScheduleFragment.setArguments(args);

        return dailyScheduleFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                .customView(R.layout.row_daily_schedule_dialog, false)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onPositive((dialog, which) -> {
                    Assert.assertTrue(mCustomTimeDatas != null);
                    Assert.assertTrue(mTimePairPersist != null);

                    mDailyScheduleDialogListener.onDailyScheduleDialogResult(mTimePairPersist);
                })
                .build();

        View view = materialDialog.getCustomView();
        Assert.assertTrue(view != null);

        mDailyScheduleDialogTime = (TextView) view.findViewById(R.id.daily_schedule_dialog_time);
        Assert.assertTrue(mDailyScheduleDialogTime != null);

        return materialDialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        Assert.assertTrue(args != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(TIME_PAIR_PERSIST_STATE_KEY));
            mTimePairPersist = savedInstanceState.getParcelable(TIME_PAIR_PERSIST_STATE_KEY);
            Assert.assertTrue(mTimePairPersist != null);
        } else {
            Assert.assertTrue(args.containsKey(TIME_PAIR_PERSIST_KEY));
            mTimePairPersist = args.getParcelable(TIME_PAIR_PERSIST_KEY);
            Assert.assertTrue(mTimePairPersist != null);
        }

        mDailyScheduleDialogTime.setOnClickListener(v -> {
            Assert.assertTrue(mCustomTimeDatas != null);

            ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas = Stream.of(mCustomTimeDatas.values())
                    .sortBy(customTimeData -> customTimeData.Id)
                    .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name))
                    .collect(Collectors.toCollection(ArrayList::new));

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

    public void initialize(Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas, DailyScheduleDialogListener dailyScheduleDialogListener) {
        Assert.assertTrue(customTimeDatas != null);
        Assert.assertTrue(dailyScheduleDialogListener != null);

        mCustomTimeDatas = customTimeDatas;
        mDailyScheduleDialogListener = dailyScheduleDialogListener;

        if (getActivity() != null)
            initialize();
    }

    private void initialize() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mDailyScheduleDialogListener != null);
        Assert.assertTrue(mTimePairPersist != null);
        Assert.assertTrue(mDailyScheduleDialogTime != null);

        updateTime();
    }

    private void updateTime() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mTimePairPersist != null);

        if (mTimePairPersist.getCustomTimeId() != null) {
            ScheduleLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            mDailyScheduleDialogTime.setText(customTimeData.Name);
        } else {
            mDailyScheduleDialogTime.setText(mTimePairPersist.getHourMinute().toString());
        }
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("DailyScheduleDialogFragment.onResume");

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Assert.assertTrue(mTimePairPersist != null);

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

    public interface DailyScheduleDialogListener {
        void onDailyScheduleDialogResult(TimePairPersist timePairPersist);
    }
}