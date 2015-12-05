package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.DailyScheduleFragment;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;

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

    public SingleSchedule createSingleSchedule(RootTask rootTask, Date date, CustomTime customTime, HourMinute hourMinute) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(date != null);
        Assert.assertTrue((customTime == null) != (hourMinute == null));

        SingleSchedule singleSchedule = SingleScheduleFactory.getInstance().createSingleSchedule(rootTask, date, customTime, hourMinute);
        Assert.assertTrue(singleSchedule != null);

        return singleSchedule;
    }

    public DailySchedule createDailySchedule(RootTask rootTask, ArrayList<DailyScheduleFragment.TimeEntry> timeEntries) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(timeEntries != null);
        Assert.assertTrue(!timeEntries.isEmpty());

        DailySchedule dailySchedule = DailyScheduleFactory.getInstance().createDailySchedule(rootTask, timeEntries);
        Assert.assertTrue(dailySchedule != null);

        return dailySchedule;
    }
}
