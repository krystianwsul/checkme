package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/14/2015.
 */
public class ScheduleFactory {
    private static ScheduleFactory sInstance;

    public static ScheduleFactory getInstance() {
        if (sInstance == null)
            sInstance = new ScheduleFactory();
        return sInstance;
    }

    private ScheduleFactory() {}

    public ArrayList<Schedule> getSchedules(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        SingleSchedule singleSchedule = SingleScheduleFactory.getInstance().getSingleSchedule(rootTask);
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
                DailySchedule dailySchedule = DailyScheduleFactory.getInstance().getDailySchedule(dailyScheduleId, rootTask);
                Assert.assertTrue(dailySchedule != null);
                dailySchedules.add(dailySchedule);
            }
            return dailySchedules;
        }

        ArrayList<Integer> weeklyScheduleIds = persistenceManger.getWeeklyScheduleIds(rootTask.getId());
        if (!weeklyScheduleIds.isEmpty()) {
            ArrayList<Schedule> weeklySchedules = new ArrayList<>();
            for (int weeklyScheduleId : weeklyScheduleIds) {
                WeeklySchedule weeklySchedule = WeeklyScheduleFactory.getInstance().getWeeklySchedule(weeklyScheduleId, rootTask);
                Assert.assertTrue(weeklySchedule != null);
                weeklySchedules.add(weeklySchedule);
            }
            return weeklySchedules;
        }

        throw new IllegalArgumentException("no schedule for rootTask == " + rootTask);
    }
}
