package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetitionFactory;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayTimeRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
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
            return createWeeklyScheduleDayTime(weeklyScheduleDayTimeId, weeklySchedule);
    }

    private WeeklyScheduleDayTime createWeeklyScheduleDayTime(int weeklyScheduleDayTimeId, WeeklySchedule weeklySchedule) {
        WeeklyScheduleDayTimeRecord weeklyScheduleDayTimeRecord = PersistenceManger.getInstance().getWeeklyScheduleDayTimeRecord(weeklyScheduleDayTimeId);
        Assert.assertTrue(weeklyScheduleDayTimeRecord != null);

        WeeklyScheduleDayTime weeklyScheduleDayTime = new WeeklyScheduleDayTime(weeklyScheduleDayTimeRecord, weeklySchedule);
        WeeklyRepetitionFactory.getInstance().loadExistingWeeklyRepetitions(weeklyScheduleDayTime);

        mWeeklyScheduleDayTimes.put(weeklyScheduleDayTimeId, weeklyScheduleDayTime);
        return weeklyScheduleDayTime;
    }
}
