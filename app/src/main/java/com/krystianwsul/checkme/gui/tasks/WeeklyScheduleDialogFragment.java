package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Map;

public class WeeklyScheduleDialogFragment extends ScheduleDialogFragment {
    private WeeklyScheduleDialogListener mWeeklyScheduleDialogListener;

    @NonNull
    public static WeeklyScheduleDialogFragment newInstance(@NonNull DayOfWeek dayOfWeek, @NonNull TimePairPersist timePairPersist) {
        WeeklyScheduleDialogFragment weeklyScheduleFragment = new WeeklyScheduleDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(DATE_KEY, null);
        args.putSerializable(DAY_OF_WEEK_KEY, dayOfWeek);
        args.putParcelable(TIME_PAIR_PERSIST_KEY, timePairPersist.copy());
        weeklyScheduleFragment.setArguments(args);

        return weeklyScheduleFragment;
    }

    @Override
    protected void onPositive() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mWeeklyScheduleDialogListener != null);

        mWeeklyScheduleDialogListener.onWeeklyScheduleDialogResult(mDayOfWeek, mTimePairPersist);
    }

    public void initialize(@NonNull Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas, @NonNull WeeklyScheduleDialogListener weeklyScheduleDialogListener) {
        initialize(customTimeDatas);

        mWeeklyScheduleDialogListener = weeklyScheduleDialogListener;

        if (getActivity() != null)
            initialize();
    }

    @Override
    protected void initialize() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mWeeklyScheduleDialogListener != null);
        Assert.assertTrue(getActivity() != null);

        mScheduleDialogDay.setVisibility(View.VISIBLE);
        mScheduleDialogTimeLayout.setVisibility(View.VISIBLE);

        updateFields();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void updateFields() {
        if (mTimePairPersist.getCustomTimeId() != null) {
            ScheduleLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            mScheduleDialogTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")");
        } else {
            mScheduleDialogTime.setText(mTimePairPersist.getHourMinute().toString());
        }
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("WeeklyScheduleDialogFragment.onResume");

        super.onResume();
    }

    public interface WeeklyScheduleDialogListener {
        void onWeeklyScheduleDialogResult(@NonNull DayOfWeek dayOfWeek, @NonNull TimePairPersist timePairPersist);
    }

    @Override
    @NonNull
    protected ScheduleType getScheduleType() {
        return ScheduleType.WEEKLY;
    }
}
