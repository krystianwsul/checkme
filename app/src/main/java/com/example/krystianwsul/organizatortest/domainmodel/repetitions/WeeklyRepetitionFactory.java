package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleDayTime;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyRepetitionRecord;

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

    public WeeklyRepetition getWeeklyRepetition(int weeklyRepetitionId, WeeklyScheduleDayTime weeklyScheduleDayTime) {
        if (mWeeklyRepetitions.containsKey(weeklyRepetitionId)) {
            return mWeeklyRepetitions.get(weeklyRepetitionId);
        } else {
            WeeklyRepetition weeklyRepetition = new RealWeeklyRepetition(PersistenceManger.getInstance().getWeeklyRepetitionRecord(weeklyRepetitionId), weeklyScheduleDayTime);
            mWeeklyRepetitions.put(weeklyRepetitionId, weeklyRepetition);
            return weeklyRepetition;
        }
    }

    public WeeklyRepetition getWeeklyRepetition(WeeklyScheduleDayTime weeklyScheduleDayTime, Date scheduleDate) {
        WeeklyRepetition existingWeeklyRepetition = getExistingWeeklyRepetition(weeklyScheduleDayTime.getId(), scheduleDate);
        if (existingWeeklyRepetition != null)
            return existingWeeklyRepetition;

        WeeklyRepetitionRecord weeklyRepetitionRecord = PersistenceManger.getInstance().getWeeklyRepetitionRecord(weeklyScheduleDayTime.getId(), scheduleDate);
        if (weeklyRepetitionRecord != null) {
            RealWeeklyRepetition realWeeklyRepetition = new RealWeeklyRepetition(weeklyRepetitionRecord, weeklyScheduleDayTime);
            mWeeklyRepetitions.put(realWeeklyRepetition.getId(), realWeeklyRepetition);
            return realWeeklyRepetition;
        }

        VirtualWeeklyRepetition virtualWeeklyRepetition = new VirtualWeeklyRepetition(weeklyScheduleDayTime, scheduleDate);
        mWeeklyRepetitions.put(virtualWeeklyRepetition.getId(), virtualWeeklyRepetition);
        return virtualWeeklyRepetition;
    }

    private WeeklyRepetition getExistingWeeklyRepetition(int weeklyScheduleDayTimeId, Date scheduleDate) {
        for (WeeklyRepetition weeklyRepetition : mWeeklyRepetitions.values())
            if (weeklyRepetition.getWeeklyScheduleDayTimeId() == weeklyScheduleDayTimeId && weeklyRepetition.getScheduleDate() == scheduleDate)
                return weeklyRepetition;
        return null;
    }
}
