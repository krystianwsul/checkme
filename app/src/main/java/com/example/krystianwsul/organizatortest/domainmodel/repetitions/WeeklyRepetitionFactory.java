package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleDayTime;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyRepetitionRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public class WeeklyRepetitionFactory {
    private static WeeklyRepetitionFactory sInstance;

    public static WeeklyRepetitionFactory getInstance() {
        if (sInstance == null)
            sInstance = new WeeklyRepetitionFactory();
        return sInstance;
    }

    private WeeklyRepetitionFactory() {}

    private final HashMap<Integer, WeeklyRepetition> mWeeklyRepetitions = new HashMap<>();

    public void loadExistingWeeklyRepetitions(WeeklyScheduleDayTime weeklyScheduleDayTime) {
        ArrayList<WeeklyRepetitionRecord> weeklyRepetitionRecords = PersistenceManger.getInstance().getWeeklyRepetitionRecords(weeklyScheduleDayTime.getId());
        for (WeeklyRepetitionRecord weeklyRepetitionRecord : weeklyRepetitionRecords) {
            WeeklyRepetition weeklyRepetition = new WeeklyRepetition(weeklyScheduleDayTime, weeklyRepetitionRecord);
            mWeeklyRepetitions.put(weeklyRepetition.getId(), weeklyRepetition);
        }
    }

    public WeeklyRepetition getWeeklyRepetition(int weeklyRepetitionId) {
        Assert.assertTrue(mWeeklyRepetitions.containsKey(weeklyRepetitionId));
        return mWeeklyRepetitions.get(weeklyRepetitionId);
    }

    public WeeklyRepetition getWeeklyRepetition(WeeklyScheduleDayTime weeklyScheduleDayTime, Date scheduleDate) {
        Assert.assertTrue(weeklyScheduleDayTime != null);
        Assert.assertTrue(scheduleDate != null);

        WeeklyRepetition existingWeeklyRepetition = getExistingWeeklyRepetition(weeklyScheduleDayTime.getId(), scheduleDate);
        if (existingWeeklyRepetition != null)
            return existingWeeklyRepetition;

        WeeklyRepetition weeklyRepetition = new WeeklyRepetition(weeklyScheduleDayTime, scheduleDate);
        mWeeklyRepetitions.put(weeklyRepetition.getId(), weeklyRepetition);
        return weeklyRepetition;
    }

    private WeeklyRepetition getExistingWeeklyRepetition(int weeklyScheduleDayTimeId, Date scheduleDate) {
        Assert.assertTrue(scheduleDate != null);

        for (WeeklyRepetition weeklyRepetition : mWeeklyRepetitions.values())
            if (weeklyRepetition.getWeeklyScheduleDayTimeId() == weeklyScheduleDayTimeId && weeklyRepetition.getScheduleDate().equals(scheduleDate))
                return weeklyRepetition;
        return null;
    }
}
