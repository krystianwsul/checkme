package com.krystianwsul.checkme.gui.tasks;

import android.os.Bundle;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.utils.time.DayOfWeek;

import junit.framework.Assert;

public class WeeklyScheduleFragment extends RepeatingScheduleFragment {
    public static WeeklyScheduleFragment newInstance() {
        return new WeeklyScheduleFragment();
    }

    public static WeeklyScheduleFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        WeeklyScheduleFragment weeklyScheduleFragment = new WeeklyScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);
        weeklyScheduleFragment.setArguments(args);

        return weeklyScheduleFragment;
    }

    public static WeeklyScheduleFragment newInstance(int rootTaskId) {
        WeeklyScheduleFragment weeklyScheduleFragment = new WeeklyScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        weeklyScheduleFragment.setArguments(args);
        return weeklyScheduleFragment;
    }

    @Override
    protected ScheduleEntry firstScheduleEntry(boolean showDelete) {
        if (mScheduleHint != null) {
            if (mScheduleHint.mTimePair != null) {
                return new WeeklyScheduleEntry(mScheduleHint.mDate.getDayOfWeek(), mScheduleHint.mTimePair, showDelete);
            } else {
                return new WeeklyScheduleEntry(mScheduleHint.mDate.getDayOfWeek(), showDelete);
            }
        } else {
            return new WeeklyScheduleEntry(DayOfWeek.today(), showDelete);
        }
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("WeeklyScheduleFragment.onResume");

        super.onResume();
    }
}
