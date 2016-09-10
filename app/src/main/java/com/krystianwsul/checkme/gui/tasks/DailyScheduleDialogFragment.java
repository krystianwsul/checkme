package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

public class DailyScheduleDialogFragment extends ScheduleDialogFragment {
    @NonNull
    public static DailyScheduleDialogFragment newInstance(@NonNull ScheduleDialogData scheduleDialogData) {
        DailyScheduleDialogFragment dailyScheduleFragment = new DailyScheduleDialogFragment();

        Assert.assertTrue(scheduleDialogData.mDate == null);
        Assert.assertTrue(scheduleDialogData.mDayOfWeek == null);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist != null);

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_DIALOG_DATA_KEY, scheduleDialogData);
        dailyScheduleFragment.setArguments(args);

        return dailyScheduleFragment;
    }

    @Override
    protected void initialize() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mScheduleDialogListener != null);
        Assert.assertTrue(mTimePairPersist != null);
        Assert.assertTrue(mScheduleDialogTime != null);
        Assert.assertTrue(getActivity() != null);

        mScheduleDialogTimeLayout.setVisibility(View.VISIBLE);

        updateFields();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void updateFields() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mTimePairPersist != null);

        if (mTimePairPersist.getCustomTimeId() != null) {
            ScheduleLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            mScheduleDialogTime.setText(customTimeData.Name);
        } else {
            mScheduleDialogTime.setText(mTimePairPersist.getHourMinute().toString());
        }
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("DailyScheduleDialogFragment.onResume");

        super.onResume();
    }

    @Override
    @NonNull
    protected ScheduleType getScheduleType() {
        return ScheduleType.DAILY;
    }

    @Override
    protected boolean isValid() {
        return true;
    }
}