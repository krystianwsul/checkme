package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

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

    public DailyRepetition getDailyRepetition(int dailyRepetitionId, DailyScheduleTime dailyScheduleTime) {
        if (mDailyRepetitions.containsKey(dailyRepetitionId)) {
            return mDailyRepetitions.get(dailyRepetitionId);
        } else {
            DailyRepetition dailyRepetition = new RealDailyRepetition(PersistenceManger.getInstance().getDailyRepetitionRecord(dailyRepetitionId), dailyScheduleTime);
            mDailyRepetitions.put(dailyRepetitionId, dailyRepetition);
            return dailyRepetition;
        }
    }

    public DailyRepetition getDailyRepetition(DailyScheduleTime dailyScheduleTime, Date scheduleDate) {
        DailyRepetition existingDailyRepetition = getExistingDailyRepetition(dailyScheduleTime.getId(), scheduleDate);
        if (existingDailyRepetition != null)
            return existingDailyRepetition;

        DailyRepetitionRecord dailyRepetitionRecord = PersistenceManger.getInstance().getDailyRepetitionRecord(dailyScheduleTime.getId(), scheduleDate);
        if (dailyRepetitionRecord != null) {
            RealDailyRepetition realDailyRepetition = new RealDailyRepetition(dailyRepetitionRecord, dailyScheduleTime);
            mDailyRepetitions.put(realDailyRepetition.getId(), realDailyRepetition);
            return realDailyRepetition;
        }

        VirtualDailyRepetition virtualDailyRepetition = new VirtualDailyRepetition(dailyScheduleTime, scheduleDate);
        mDailyRepetitions.put(virtualDailyRepetition.getId(), virtualDailyRepetition);
        return virtualDailyRepetition;
    }

    private DailyRepetition getExistingDailyRepetition(int dailyScheduleTimeId, Date scheduleDate) {
        for (DailyRepetition dailyRepetition : mDailyRepetitions.values())
            if (dailyRepetition.getDailyScheduleTimeId() == dailyScheduleTimeId && dailyRepetition.getScheduleDate() == scheduleDate)
                return dailyRepetition;
        return null;
    }
}
