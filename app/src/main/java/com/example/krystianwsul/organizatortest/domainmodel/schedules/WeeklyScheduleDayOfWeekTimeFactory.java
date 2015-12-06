package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;

import junit.framework.Assert;

import java.util.HashMap;

public class WeeklyScheduleDayOfWeekTimeFactory {
    private static WeeklyScheduleDayOfWeekTimeFactory sInstance;

    public static WeeklyScheduleDayOfWeekTimeFactory getInstance() {
        if (sInstance == null)
            sInstance = new WeeklyScheduleDayOfWeekTimeFactory();
        return sInstance;
    }

    private WeeklyScheduleDayOfWeekTimeFactory() {}

    private final HashMap<Integer, WeeklyScheduleDayOfWeekTime> mWeeklyScheduleDayOfWeekTimes = new HashMap<>();

    public WeeklyScheduleDayOfWeekTime getWeeklyScheduleDayOfWeekTime(int weeklyScheduleDayOfWeekTimeId, WeeklySchedule weeklySchedule) {
        if (mWeeklyScheduleDayOfWeekTimes.containsKey(weeklyScheduleDayOfWeekTimeId))
            return mWeeklyScheduleDayOfWeekTimes.get(weeklyScheduleDayOfWeekTimeId);
        else
            return loadWeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeId, weeklySchedule);
    }

    private WeeklyScheduleDayOfWeekTime loadWeeklyScheduleDayOfWeekTime(int weeklyScheduleDayOfWeekTimeId, WeeklySchedule weeklySchedule) {
        WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = PersistenceManger.getInstance().getWeeklyScheduleDayOfWeekTimeRecord(weeklyScheduleDayOfWeekTimeId);
        Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecord != null);

        WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime = new WeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeRecord, weeklySchedule);
        WeeklyRepetitionFactory.getInstance().loadExistingWeeklyRepetitions(weeklyScheduleDayOfWeekTime);

        mWeeklyScheduleDayOfWeekTimes.put(weeklyScheduleDayOfWeekTimeId, weeklyScheduleDayOfWeekTime);
        return weeklyScheduleDayOfWeekTime;
    }

    public WeeklyScheduleDayOfWeekTime createWeeklyScheduleDayOfWeekTime(WeeklySchedule weeklySchedule, DayOfWeek dayOfWeek, CustomTime customTime, HourMinute hourMinute) {
        Assert.assertTrue(weeklySchedule != null);
        Assert.assertTrue(dayOfWeek != null);
        Assert.assertTrue((customTime == null) != (hourMinute == null));

        WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = PersistenceManger.getInstance().createWeeklyScheduleDayOfWeekTimeRecord(weeklySchedule, dayOfWeek, customTime, hourMinute);
        Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecord != null);

        WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime = new WeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeRecord, weeklySchedule);
        mWeeklyScheduleDayOfWeekTimes.put(weeklyScheduleDayOfWeekTime.getId(), weeklyScheduleDayOfWeekTime);

        return weeklyScheduleDayOfWeekTime;
    }
}
