package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.HashMap;

public class DailyScheduleTimeFactory {
    private static DailyScheduleTimeFactory sInstance;

    public static DailyScheduleTimeFactory getInstance() {
        if (sInstance == null)
            sInstance = new DailyScheduleTimeFactory();
        return sInstance;
    }

    private DailyScheduleTimeFactory() {}

    private final HashMap<Integer, DailyScheduleTime> mDailyScheduleTimes = new HashMap<>();

    public DailyScheduleTime getDailyScheduleTime(int dailyScheduleTimeId, DailySchedule dailySchedule) {
        if (mDailyScheduleTimes.containsKey(dailyScheduleTimeId))
            return mDailyScheduleTimes.get(dailyScheduleTimeId);
        else
            return loadDailyScheduleTime(dailyScheduleTimeId, dailySchedule);
    }

    private DailyScheduleTime loadDailyScheduleTime(int dailyScheduleTimeId, DailySchedule dailySchedule) {
        DailyScheduleTimeRecord dailyScheduleTimeRecord = PersistenceManger.getInstance().getDailyScheduleTimeRecord(dailyScheduleTimeId);
        Assert.assertTrue(dailyScheduleTimeRecord != null);

        DailyScheduleTime dailyScheduleTime = new DailyScheduleTime(dailyScheduleTimeRecord, dailySchedule);
        DailyRepetitionFactory.getInstance().loadExistingDailyRepetitions(dailyScheduleTime);

        mDailyScheduleTimes.put(dailyScheduleTime.getId(), dailyScheduleTime);
        return dailyScheduleTime;
    }

    public DailyScheduleTime createDailyScheduleTime(DailySchedule dailySchedule, CustomTime customTime, HourMinute hourMinute) {
        Assert.assertTrue(dailySchedule != null);
        Assert.assertTrue((customTime == null) != (hourMinute == null));

        DailyScheduleTimeRecord dailyScheduleTimeRecord = PersistenceManger.getInstance().createDailyScheduleTimeRecord(dailySchedule, customTime, hourMinute);
        Assert.assertTrue(dailyScheduleTimeRecord != null);

        DailyScheduleTime dailyScheduleTime = new DailyScheduleTime(dailyScheduleTimeRecord, dailySchedule);
        mDailyScheduleTimes.put(dailyScheduleTime.getId(), dailyScheduleTime);

        return dailyScheduleTime;
    }
}
