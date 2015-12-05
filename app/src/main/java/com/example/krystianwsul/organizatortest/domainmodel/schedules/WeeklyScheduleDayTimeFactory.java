package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayTimeRecord;

import junit.framework.Assert;

import java.util.HashMap;

public class WeeklyScheduleDayTimeFactory {
    private static WeeklyScheduleDayTimeFactory sInstance;

    public static WeeklyScheduleDayTimeFactory getInstance() {
        if (sInstance == null)
            sInstance = new WeeklyScheduleDayTimeFactory();
        return sInstance;
    }

    private WeeklyScheduleDayTimeFactory() {}

    private final HashMap<Integer, WeeklyScheduleDayTime> mWeeklyScheduleDayTimes = new HashMap<>();

    public WeeklyScheduleDayTime getWeeklyScheduleDayTime(int weeklyScheduleDayTimeId, WeeklySchedule weeklySchedule) {
        if (mWeeklyScheduleDayTimes.containsKey(weeklyScheduleDayTimeId))
            return mWeeklyScheduleDayTimes.get(weeklyScheduleDayTimeId);
        else
            return loadWeeklyScheduleDayTime(weeklyScheduleDayTimeId, weeklySchedule);
    }

    private WeeklyScheduleDayTime loadWeeklyScheduleDayTime(int weeklyScheduleDayTimeId, WeeklySchedule weeklySchedule) {
        WeeklyScheduleDayTimeRecord weeklyScheduleDayTimeRecord = PersistenceManger.getInstance().getWeeklyScheduleDayTimeRecord(weeklyScheduleDayTimeId);
        Assert.assertTrue(weeklyScheduleDayTimeRecord != null);

        WeeklyScheduleDayTime weeklyScheduleDayTime = new WeeklyScheduleDayTime(weeklyScheduleDayTimeRecord, weeklySchedule);
        WeeklyRepetitionFactory.getInstance().loadExistingWeeklyRepetitions(weeklyScheduleDayTime);

        mWeeklyScheduleDayTimes.put(weeklyScheduleDayTimeId, weeklyScheduleDayTime);
        return weeklyScheduleDayTime;
    }

    public WeeklyScheduleDayTime createWeeklyScheduleDayTime(WeeklySchedule weeklySchedule, DayOfWeek dayOfWeek, CustomTime customTime, HourMinute hourMinute) {
        Assert.assertTrue(weeklySchedule != null);
        Assert.assertTrue(dayOfWeek != null);
        Assert.assertTrue((customTime == null) != (hourMinute == null));

        WeeklyScheduleDayTimeRecord weeklyScheduleDayTimeRecord = PersistenceManger.getInstance().createWeeklyScheduleDayTimeRecord(weeklySchedule, dayOfWeek, customTime, hourMinute);
        Assert.assertTrue(weeklyScheduleDayTimeRecord != null);

        WeeklyScheduleDayTime weeklyScheduleDayTime = new WeeklyScheduleDayTime(weeklyScheduleDayTimeRecord, weeklySchedule);
        mWeeklyScheduleDayTimes.put(weeklyScheduleDayTime.getId(), weeklyScheduleDayTime);

        return weeklyScheduleDayTime;
    }
}
