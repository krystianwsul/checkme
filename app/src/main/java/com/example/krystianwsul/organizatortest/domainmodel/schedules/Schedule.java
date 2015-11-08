package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/17/2015.
 */
public abstract class Schedule {
    protected final Task mTask;

    public abstract String getTaskText(Context context);
    public abstract ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp);

    public static Schedule getSchedule(Task task) {
        Assert.assertTrue(task != null);
        SingleSchedule singleSchedule = SingleSchedule.getSingleSchedule(task);
        if (singleSchedule != null)
            return singleSchedule;
        DailySchedule dailySchedule = DailySchedule.getDailySchedule(task);
        if (dailySchedule != null)
            return dailySchedule;
        throw new IllegalArgumentException("no schedule for task == " + task);
    }

    protected Schedule(Task task) {
        Assert.assertTrue(task != null);
        mTask = task;
    }
}
