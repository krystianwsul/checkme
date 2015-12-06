package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.WeeklyScheduleFragment;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
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

        ArrayList<Integer> weeklyScheduleDayTimeIds = persistenceManger.getWeeklyScheduleDayTimeIds(rootTask.getId());
        Assert.assertTrue(!weeklyScheduleDayTimeIds.isEmpty());

        for (Integer weeklyScheduleDayTimeId : weeklyScheduleDayTimeIds)
            weeklySchedule.addWeeklyScheduleDayTime(WeeklyScheduleDayTimeFactory.getInstance().getWeeklyScheduleDayTime(weeklyScheduleDayTimeId, weeklySchedule));

        mWeeklySchedules.put(rootTask.getId(), weeklySchedule);
        return weeklySchedule;
    }

    public WeeklySchedule createWeeklySchedule(RootTask rootTask, ArrayList<WeeklyScheduleFragment.DayOfWeekTimeEntry> dayOfWeekTimeEntries) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(dayOfWeekTimeEntries != null);
        Assert.assertTrue(!dayOfWeekTimeEntries.isEmpty());

        WeeklyScheduleRecord weeklyScheduleRecord = PersistenceManger.getInstance().createWeeklyScheduleRecord(rootTask.getId());
        Assert.assertTrue(weeklyScheduleRecord != null);

        WeeklySchedule weeklySchedule = new WeeklySchedule(weeklyScheduleRecord, rootTask);

        WeeklyScheduleDayTimeFactory weeklyScheduleDayTimeFactory = WeeklyScheduleDayTimeFactory.getInstance();

        for (WeeklyScheduleFragment.DayOfWeekTimeEntry dayOfWeekTimeEntry : dayOfWeekTimeEntries)
            weeklySchedule.addWeeklyScheduleDayTime(weeklyScheduleDayTimeFactory.createWeeklyScheduleDayTime(weeklySchedule, dayOfWeekTimeEntry.getDayOfWeek(), dayOfWeekTimeEntry.getCustomTime(), dayOfWeekTimeEntry.getHourMinute()));

        mWeeklySchedules.put(weeklySchedule.getRootTaskId(), weeklySchedule);
        return weeklySchedule;
    }
}
