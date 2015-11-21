package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetitionFactory;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
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
            return createDailyScheduleTime(dailyScheduleTimeId, dailySchedule);
    }

    private DailyScheduleTime createDailyScheduleTime(int dailyScheduleTimeId, DailySchedule dailySchedule) {
        DailyScheduleTimeRecord dailyScheduleTimeRecord = PersistenceManger.getInstance().getDailyScheduleTimeRecord(dailyScheduleTimeId);
        Assert.assertTrue(dailyScheduleTimeRecord != null);

        DailyScheduleTime dailyScheduleTime = new DailyScheduleTime(dailyScheduleTimeRecord, dailySchedule);
        DailyRepetitionFactory.getInstance().loadExistingDailyRepetitions(dailyScheduleTime);

        mDailyScheduleTimes.put(dailyScheduleTimeId, dailyScheduleTime);
        return dailyScheduleTime;
    }
}
