package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.WeeklyScheduleDayOfWeekTime;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyRepetitionRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class WeeklyRepetitionFactory {
    private static WeeklyRepetitionFactory sInstance;

    public static WeeklyRepetitionFactory getInstance() {
        if (sInstance == null)
            sInstance = new WeeklyRepetitionFactory();
        return sInstance;
    }

    private WeeklyRepetitionFactory() {}

    private final HashMap<Integer, WeeklyRepetition> mWeeklyRepetitions = new HashMap<>();

    public void loadExistingWeeklyRepetitions(WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime) {
        ArrayList<WeeklyRepetitionRecord> weeklyRepetitionRecords = PersistenceManger.getInstance().getWeeklyRepetitionRecords(weeklyScheduleDayOfWeekTime);
        for (WeeklyRepetitionRecord weeklyRepetitionRecord : weeklyRepetitionRecords) {
            WeeklyRepetition weeklyRepetition = new WeeklyRepetition(weeklyScheduleDayOfWeekTime, weeklyRepetitionRecord);
            mWeeklyRepetitions.put(weeklyRepetition.getId(), weeklyRepetition);
        }
    }

    public WeeklyRepetition getWeeklyRepetition(int weeklyRepetitionId) {
        Assert.assertTrue(mWeeklyRepetitions.containsKey(weeklyRepetitionId));
        return mWeeklyRepetitions.get(weeklyRepetitionId);
    }

    public WeeklyRepetition getWeeklyRepetition(WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime, Date scheduleDate) {
        Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);
        Assert.assertTrue(scheduleDate != null);

        WeeklyRepetition existingWeeklyRepetition = getExistingWeeklyRepetition(weeklyScheduleDayOfWeekTime.getId(), scheduleDate);
        if (existingWeeklyRepetition != null)
            return existingWeeklyRepetition;

        WeeklyRepetition weeklyRepetition = new WeeklyRepetition(weeklyScheduleDayOfWeekTime, scheduleDate);
        mWeeklyRepetitions.put(weeklyRepetition.getId(), weeklyRepetition);
        return weeklyRepetition;
    }

    private WeeklyRepetition getExistingWeeklyRepetition(int weeklyScheduleDayOfWeekTimeId, Date scheduleDate) {
        Assert.assertTrue(scheduleDate != null);

        for (WeeklyRepetition weeklyRepetition : mWeeklyRepetitions.values())
            if (weeklyRepetition.getWeeklyScheduleDayOfWeekTimeId() == weeklyScheduleDayOfWeekTimeId && weeklyRepetition.getScheduleDate().equals(scheduleDate))
                return weeklyRepetition;
        return null;
    }
}
