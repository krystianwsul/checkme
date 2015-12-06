package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.support.v4.util.Pair;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

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

    public Schedule getSchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        SingleSchedule singleSchedule = SingleScheduleFactory.getInstance().getSingleSchedule(rootTask);
        if (singleSchedule != null)
            return singleSchedule;

        DailySchedule dailySchedule = DailyScheduleFactory.getInstance().getDailySchedule(rootTask);
        if (dailySchedule != null)
            return dailySchedule;

        WeeklySchedule weeklySchedule = WeeklyScheduleFactory.getInstance().getWeeklySchedule(rootTask);
        if (weeklySchedule != null)
            return weeklySchedule;

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

    public DailySchedule createDailySchedule(RootTask rootTask, ArrayList<Pair<CustomTime, HourMinute>> timePairs) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        DailySchedule dailySchedule = DailyScheduleFactory.getInstance().createDailySchedule(rootTask, timePairs);
        Assert.assertTrue(dailySchedule != null);

        return dailySchedule;
    }

    public WeeklySchedule createWeeklySchedule(RootTask rootTask, ArrayList<Pair<DayOfWeek, Pair<CustomTime, HourMinute>>> dayOfWeekTimePairs) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        WeeklySchedule weeklySchedule = WeeklyScheduleFactory.getInstance().createWeeklySchedule(rootTask, dayOfWeekTimePairs);
        Assert.assertTrue(weeklySchedule != null);

        return weeklySchedule;
    }

    public Schedule copy(Schedule schedule, RootTask newRootTask) {
        Assert.assertTrue(schedule != null);
        Assert.assertTrue(newRootTask != null);

        return schedule.copy(newRootTask);
    }
}
