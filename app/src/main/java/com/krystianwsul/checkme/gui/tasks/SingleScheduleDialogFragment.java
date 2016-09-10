package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

public class SingleScheduleDialogFragment extends ScheduleDialogFragment {
    @NonNull
    public static SingleScheduleDialogFragment newInstance(@NonNull ScheduleDialogData scheduleDialogData) {
        SingleScheduleDialogFragment singleScheduleFragment = new SingleScheduleDialogFragment();

        Assert.assertTrue(scheduleDialogData.mDate != null);
        Assert.assertTrue(scheduleDialogData.mDayOfWeek == null);
        Assert.assertTrue(scheduleDialogData.mTimePairPersist != null);

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_DIALOG_DATA_KEY, scheduleDialogData);
        singleScheduleFragment.setArguments(args);

        return singleScheduleFragment;
    }

    @Override
    protected void initialize() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mScheduleDialogListener != null);
        Assert.assertTrue(mTimePairPersist != null);
        Assert.assertTrue(mScheduleDialogTime != null);
        Assert.assertTrue(getActivity() != null);

        mScheduleDialogDateLayout.setVisibility(View.VISIBLE);
        mScheduleDialogTimeLayout.setVisibility(View.VISIBLE);

        updateFields();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void updateFields() {
        Assert.assertTrue(mDate != null);
        Assert.assertTrue(mScheduleDialogDate != null);
        Assert.assertTrue(mTimePairPersist != null);
        Assert.assertTrue(mScheduleDialogTime != null);
        Assert.assertTrue(mCustomTimeDatas != null);

        mScheduleDialogDate.setText(mDate.getDisplayText(getContext()));

        if (mTimePairPersist.getCustomTimeId() != null) {
            ScheduleLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            mScheduleDialogTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")");
        } else {
            mScheduleDialogTime.setText(mTimePairPersist.getHourMinute().toString());
        }

        if (isValid()) {
            mButton.setEnabled(true);

            mScheduleDialogDateLayout.setError(null);
            mScheduleDialogTimeLayout.setError(null);
        } else {
            mButton.setEnabled(false);

            if (mDate.compareTo(Date.today()) >= 0) {
                mScheduleDialogDateLayout.setError(null);
                mScheduleDialogTimeLayout.setError(getString(R.string.error_time));
            } else {
                mScheduleDialogDateLayout.setError(getString(R.string.error_date));
                mScheduleDialogTimeLayout.setError(null);
            }
        }
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("SingleScheduleDialogFragment.onResume");

        super.onResume();
    }

    @Override
    @NonNull
    protected ScheduleType getScheduleType() {
        return ScheduleType.SINGLE;
    }

    @Override
    protected boolean isValid() {
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
}