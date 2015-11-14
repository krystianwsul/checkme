package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/17/2015.
 */
public abstract class Schedule {
    protected final RootTask mRootTask;

    public abstract String getTaskText(Context context);
    public abstract ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp);

    public static Schedule getSchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        SingleSchedule singleSchedule = SingleSchedule.getSingleSchedule(rootTask);
        if (singleSchedule != null)
            return singleSchedule;
        DailySchedule dailySchedule = DailySchedule.getDailySchedule(rootTask);
        if (dailySchedule != null)
            return dailySchedule;
        WeeklySchedule weeklySchedule = WeeklySchedule.getWeeklySchedule(rootTask);
        if (weeklySchedule != null)
            return weeklySchedule;
        throw new IllegalArgumentException("no schedule for rootTask == " + rootTask);
    }

    protected Schedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        mRootTask = rootTask;
    }

    public int getRootTaskId() {
        return mRootTask.getId();
    }
}
