package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.support.v4.util.Pair;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class WeeklyScheduleFactory {
    private static WeeklyScheduleFactory sInstance;

    public static WeeklyScheduleFactory getInstance() {
        if (sInstance == null)
            sInstance = new WeeklyScheduleFactory();
        return sInstance;
    }

    private WeeklyScheduleFactory() {}

    private final HashMap<Integer, WeeklySchedule> mWeeklySchedules = new HashMap<>();

    public WeeklySchedule getWeeklySchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        if (mWeeklySchedules.containsKey(rootTask.getId()))
            return mWeeklySchedules.get(rootTask.getId());
        else
            return loadWeeklySchedule(rootTask);
    }

    private WeeklySchedule loadWeeklySchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        WeeklyScheduleRecord weeklyScheduleRecord = persistenceManger.getWeeklyScheduleRecord(rootTask.getId());
        if (weeklyScheduleRecord == null)
            return null;

        WeeklySchedule weeklySchedule = new WeeklySchedule(weeklyScheduleRecord, rootTask);

        ArrayList<Integer> weeklyScheduleDayOfWeekTimeIds = persistenceManger.getWeeklyScheduleDayOfWeekTimeIds(rootTask.getId());
        Assert.assertTrue(!weeklyScheduleDayOfWeekTimeIds.isEmpty());

        for (Integer weeklyScheduleDayOfWeekTimeId : weeklyScheduleDayOfWeekTimeIds)
            weeklySchedule.addWeeklyScheduleDayOfWeekTime(WeeklyScheduleDayOfWeekTimeFactory.getInstance().getWeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeId, weeklySchedule));

        mWeeklySchedules.put(rootTask.getId(), weeklySchedule);
        return weeklySchedule;
    }

    public WeeklySchedule createWeeklySchedule(RootTask rootTask, ArrayList<Pair<DayOfWeek, Pair<CustomTime, HourMinute>>> dayOfWeekTimePairs) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        WeeklyScheduleRecord weeklyScheduleRecord = PersistenceManger.getInstance().createWeeklyScheduleRecord(rootTask.getId());
        Assert.assertTrue(weeklyScheduleRecord != null);

        WeeklySchedule weeklySchedule = new WeeklySchedule(weeklyScheduleRecord, rootTask);

        WeeklyScheduleDayOfWeekTimeFactory weeklyScheduleDayOfWeekTimeFactory = WeeklyScheduleDayOfWeekTimeFactory.getInstance();

        for (Pair<DayOfWeek, Pair<CustomTime, HourMinute>> dayOfWeekTimePair : dayOfWeekTimePairs) {
            DayOfWeek dayOfWeek = dayOfWeekTimePair.first;
            Assert.assertTrue(dayOfWeek != null);

            CustomTime customTime = dayOfWeekTimePair.second.first;
            HourMinute hourMinute = dayOfWeekTimePair.second.second;
            Assert.assertTrue((customTime == null) != (hourMinute == null));

            weeklySchedule.addWeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeFactory.createWeeklyScheduleDayOfWeekTime(weeklySchedule, dayOfWeek, customTime, hourMinute));
        }

        mWeeklySchedules.put(weeklySchedule.getRootTaskId(), weeklySchedule);
        return weeklySchedule;
    }

    WeeklySchedule copy(WeeklySchedule oldWeeklySchedule, RootTask newRootTask) {
        Assert.assertTrue(oldWeeklySchedule != null);
        Assert.assertTrue(newRootTask != null);

        oldWeeklySchedule.setEndTimeStamp();

        ArrayList<Pair<DayOfWeek, Pair<CustomTime, HourMinute>>> dayOfWeekTimePairs = new ArrayList<>();
        Assert.assertTrue(!oldWeeklySchedule.getWeeklyScheduleDayOfWeekTimes().isEmpty());
        for (WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime : oldWeeklySchedule.getWeeklyScheduleDayOfWeekTimes()) {
            Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);
            dayOfWeekTimePairs.add(new Pair<>(weeklyScheduleDayOfWeekTime.getDayOfWeek(), weeklyScheduleDayOfWeekTime.getTime().getPair()));
        }

        return createWeeklySchedule(newRootTask, dayOfWeekTimePairs);
    }
}
