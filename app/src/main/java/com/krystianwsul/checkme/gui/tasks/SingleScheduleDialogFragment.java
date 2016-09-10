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
import com.krystianwsul.checkme.utils.time.TimePairPersist;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.Map;

public class SingleScheduleDialogFragment extends ScheduleDialogFragment {
    private SingleScheduleDialogListener mScheduleDialogListener;

    @NonNull
    public static SingleScheduleDialogFragment newInstance(@NonNull Date date, @NonNull TimePairPersist timePairPersist) {
        SingleScheduleDialogFragment singleScheduleFragment = new SingleScheduleDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(DATE_KEY, date);
        args.putSerializable(DAY_OF_WEEK_KEY, null);
        args.putParcelable(TIME_PAIR_PERSIST_KEY, timePairPersist.copy());

        singleScheduleFragment.setArguments(args);
        return singleScheduleFragment;
    }

    @Override
    protected void onPositive() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mScheduleDialogListener != null);
        Assert.assertTrue(isValidDateTime());

        mScheduleDialogListener.onSingleScheduleDialogResult(mDate, mTimePairPersist);
    }

    public void initialize(@NonNull Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas, @NonNull SingleScheduleDialogListener singleScheduleDialogListener) {
        initialize(customTimeDatas);

        mScheduleDialogListener = singleScheduleDialogListener;

        if (getActivity() != null)
            initialize();
    }

    @Override
    protected void initialize() {
        mScheduleDialogDateLayout.setVisibility(View.VISIBLE);
        mScheduleDialogTimeLayout.setVisibility(View.VISIBLE);

        updateFields();
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("SingleScheduleDialogFragment.onResume");

        super.onResume();
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

        if (isValidDateTime()) {
            mButton.setEnabled(true);

            mScheduleDialogDateLayout.setError(null);
            mScheduleDialogTimeLayout.setError(null);
        } else {
            mButton.setEnabled(false);

            if (isValidDate()) {
                mScheduleDialogDateLayout.setError(null);
                mScheduleDialogTimeLayout.setError(getString(R.string.error_time));
            } else {
                mScheduleDialogDateLayout.setError(getString(R.string.error_date));
                mScheduleDialogTimeLayout.setError(null);
            }
        }
    }

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

    public interface SingleScheduleDialogListener {
        void onSingleScheduleDialogResult(@NonNull Date date, @NonNull TimePairPersist timePairPersist);
    }

    @Override
    @NonNull
    protected ScheduleType getScheduleType() {
        return ScheduleType.SINGLE;
    }
}