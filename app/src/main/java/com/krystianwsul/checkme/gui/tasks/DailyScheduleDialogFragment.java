package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Map;

public class DailyScheduleDialogFragment extends ScheduleDialogFragment {
    private DailyScheduleDialogListener mScheduleDialogListener;

    @NonNull
    public static DailyScheduleDialogFragment newInstance(@NonNull TimePairPersist timePairPersist) {
        DailyScheduleDialogFragment dailyScheduleFragment = new DailyScheduleDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(DATE_KEY, null);
        args.putSerializable(DAY_OF_WEEK_KEY, null);
        args.putParcelable(TIME_PAIR_PERSIST_KEY, timePairPersist.copy());
        dailyScheduleFragment.setArguments(args);

        return dailyScheduleFragment;
    }

    @Override
    protected void onPositive() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mScheduleDialogListener != null);
        Assert.assertTrue(mTimePairPersist != null);

        mScheduleDialogListener.onDailyScheduleDialogResult(mTimePairPersist);
    }

    public void initialize(@NonNull Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas, @NonNull DailyScheduleDialogListener dailyScheduleDialogListener) {
        initialize(customTimeDatas);

        mScheduleDialogListener = dailyScheduleDialogListener;

        if (getActivity() != null)
            initialize();
    }

    @Override
    protected void initialize() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mScheduleDialogListener != null);
        Assert.assertTrue(mTimePairPersist != null);
        Assert.assertTrue(mScheduleDialogTime != null);

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

    public interface DailyScheduleDialogListener {
        void onDailyScheduleDialogResult(@NonNull TimePairPersist timePairPersist);
    }

    @Override
    @NonNull
    protected ScheduleType getScheduleType() {
        return ScheduleType.DAILY;
    }
}