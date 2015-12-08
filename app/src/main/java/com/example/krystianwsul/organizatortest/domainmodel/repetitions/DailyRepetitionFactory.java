package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class DailyRepetitionFactory {
    private static DailyRepetitionFactory sInstance;

    public static DailyRepetitionFactory getInstance() {
        if (sInstance == null)
            sInstance = new DailyRepetitionFactory();
        return sInstance;
    }

    private DailyRepetitionFactory() {}

    private final HashMap<Integer, DailyRepetition> mDailyRepetitions = new HashMap<>();

    public void loadExistingDailyRepetitions(DailyScheduleTime dailyScheduleTime) {
        ArrayList<DailyRepetitionRecord> dailyRepetitionRecords = PersistenceManger.getInstance().getDailyRepetitionRecords(dailyScheduleTime);
        for (DailyRepetitionRecord dailyRepetitionRecord : dailyRepetitionRecords) {
            DailyRepetition dailyRepetition = new DailyRepetition(dailyScheduleTime, dailyRepetitionRecord);
            mDailyRepetitions.put(dailyRepetition.getId(), dailyRepetition);
        }
    }

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

        DailyRepetition dailyRepetition = new DailyRepetition(dailyScheduleTime, scheduleDate);
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
}