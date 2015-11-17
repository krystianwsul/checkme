package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public class DailyRepetitionFactory {
    private static DailyRepetitionFactory sInstance;

    public static DailyRepetitionFactory getInstance() {
        if (sInstance == null)
            sInstance = new DailyRepetitionFactory();
        return sInstance;
    }

    private DailyRepetitionFactory() {}

    private final HashMap<Integer, DailyRepetition> mDailyRepetitions = new HashMap<>();

    public DailyRepetition getDailyRepetition(int dailyRepetitionId) {
        Assert.assertTrue(mDailyRepetitions.containsKey(dailyRepetitionId));
        return mDailyRepetitions.get(dailyRepetitionId);
    }

    public DailyRepetition getDailyRepetition(DailyScheduleTime dailyScheduleTime, Date scheduleDate) {
        Assert.assertTrue(dailyScheduleTime != null);
        Assert.assertTrue(scheduleDate != null);

        DailyRepetition existingDailyRepetition = getExistingDailyRepetition(dailyScheduleTime.getId(), scheduleDate);
        if (existingDailyRepetition != null)
            return existingDailyRepetition;

        DailyRepetition dailyRepetition = createDailyRepetition(dailyScheduleTime, scheduleDate);
        Assert.assertTrue(dailyRepetition != null);
        mDailyRepetitions.put(dailyRepetition.getId(), dailyRepetition);
        return dailyRepetition;
    }

    private DailyRepetition getExistingDailyRepetition(int dailyScheduleTimeId, Date scheduleDate) {
        Assert.assertTrue(scheduleDate != null);

        for (DailyRepetition dailyRepetition : mDailyRepetitions.values())
            if (dailyRepetition.getDailyScheduleTimeId() == dailyScheduleTimeId && dailyRepetition.getScheduleDate().equals(scheduleDate))
                return dailyRepetition;
        return null;
    }

    private DailyRepetition createDailyRepetition(DailyScheduleTime dailyScheduleTime, Date scheduleDate) {
        Assert.assertTrue(dailyScheduleTime != null);
        Assert.assertTrue(scheduleDate != null);

        DailyRepetitionRecord dailyRepetitionRecord = PersistenceManger.getInstance().getDailyRepetitionRecord(dailyScheduleTime.getId(), scheduleDate);
        if (dailyRepetitionRecord != null)
            return new DailyRepetition(dailyScheduleTime, dailyRepetitionRecord);
        else
            return new DailyRepetition(dailyScheduleTime, scheduleDate);
    }
}
