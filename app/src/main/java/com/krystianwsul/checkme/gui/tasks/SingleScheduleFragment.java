package com.krystianwsul.checkme.gui.tasks;

import android.os.Bundle;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.utils.time.Date;

import junit.framework.Assert;

public class SingleScheduleFragment extends ScheduleFragment {
    public static SingleScheduleFragment newInstance() {
        return new SingleScheduleFragment();
    }

    public static SingleScheduleFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);
        singleScheduleFragment.setArguments(args);

        return singleScheduleFragment;
    }

    public static SingleScheduleFragment newInstance(int rootTaskId) {
        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);
        singleScheduleFragment.setArguments(args);

        return singleScheduleFragment;
    }

    @Override
    protected ScheduleEntry firstScheduleEntry(boolean showDelete) {
        if (mScheduleHint != null) {
            if (mScheduleHint.mTimePair != null) {
                return new SingleScheduleEntry(mScheduleHint.mDate, mScheduleHint.mTimePair, showDelete);
            } else {
                return new SingleScheduleEntry(mScheduleHint.mDate, showDelete);
            }
        } else {
            return new SingleScheduleEntry(Date.today(), showDelete);
        }
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("SingleScheduleFragment.onResume");

        super.onResume();
    }
}