package com.krystianwsul.checkme.gui.tasks;

import android.os.Bundle;

import com.krystianwsul.checkme.MyCrashlytics;

import junit.framework.Assert;

public class DailyScheduleFragment extends ScheduleFragment {
    public static DailyScheduleFragment newInstance() {
        return new DailyScheduleFragment();
    }

    public static DailyScheduleFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        DailyScheduleFragment dailyScheduleFragment = new DailyScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);

        dailyScheduleFragment.setArguments(args);
        return dailyScheduleFragment;
    }

    public static DailyScheduleFragment newInstance(int rootTaskId) {
        DailyScheduleFragment dailyScheduleFragment = new DailyScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        dailyScheduleFragment.setArguments(args);
        return dailyScheduleFragment;
    }

    @Override
    protected ScheduleEntry firstScheduleEntry(boolean showDelete) {
        if (mScheduleHint != null && mScheduleHint.mTimePair != null)
            return new ScheduleEntry(mScheduleHint.mTimePair, showDelete);
        else
            return new ScheduleEntry(showDelete);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("DailyScheduleFragment.onResume");

        super.onResume();
    }
}
