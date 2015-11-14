package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/17/2015.
 */
public abstract class Schedule {
    protected final RootTask mRootTask;

    public abstract String getTaskText(Context context);
    public abstract ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp);

    public static ArrayList<Schedule> getSchedules(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        SingleSchedule singleSchedule = SingleSchedule.getSingleSchedule(rootTask);
        if (singleSchedule != null) {
            ArrayList<Schedule> singleSchedules = new ArrayList<>();
            singleSchedules.add(singleSchedule);
            return singleSchedules;
        }

        PersistenceManger persistenceManger = PersistenceManger.getInstance();
        ArrayList<Integer> dailyScheduleIds = persistenceManger.getDailyScheduleIds(rootTask.getId());
        if (!dailyScheduleIds.isEmpty()) {
            ArrayList<Schedule> dailySchedules = new ArrayList<>();
            for (int dailyScheduleId : dailyScheduleIds) {
                DailySchedule dailySchedule = DailySchedule.getDailySchedule(dailyScheduleId, rootTask);
                Assert.assertTrue(dailySchedule != null);
                dailySchedules.add(dailySchedule);
            }
            return dailySchedules;
        }

        ArrayList<Integer> weeklyScheduleIds = persistenceManger.getWeeklyScheduleIds(rootTask.getId());
        if (!weeklyScheduleIds.isEmpty()) {
            ArrayList<Schedule> weeklySchedules = new ArrayList<>();
            for (int weeklyScheduleId : weeklyScheduleIds) {
                WeeklySchedule weeklySchedule = WeeklySchedule.getWeeklySchedule(weeklyScheduleId, rootTask);
                Assert.assertTrue(weeklySchedule != null);
                weeklySchedules.add(weeklySchedule);
            }
            return weeklySchedules;
        }

        throw new IllegalArgumentException("no schedule for rootTask == " + rootTask);
    }

    protected Schedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        mRootTask = rootTask;
    }

    public int getRootTaskId() {
        return mRootTask.getId();
    }

    public abstract TimeStamp getEndTimeStamp();
}
