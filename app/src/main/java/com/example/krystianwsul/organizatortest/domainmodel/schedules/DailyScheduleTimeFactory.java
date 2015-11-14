package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

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
        if (mDailyScheduleTimes.containsKey(dailyScheduleTimeId)) {
            return mDailyScheduleTimes.get(dailyScheduleTimeId);
        } else {
            DailyScheduleTime dailyScheduleTime = createDailyScheduleTime(dailyScheduleTimeId, dailySchedule);
            mDailyScheduleTimes.put(dailyScheduleTimeId, dailyScheduleTime);
            return dailyScheduleTime;
        }
    }

    private DailyScheduleTime createDailyScheduleTime(int dailyScheduleTimeId, DailySchedule dailySchedule) {
        DailyScheduleTimeRecord dailyScheduleTimeRecord = PersistenceManger.getInstance().getDailyScheduleTimeRecord(dailyScheduleTimeId);
        if (dailyScheduleTimeRecord.getCustomTimeId() == null)
            return new DailyScheduleNormalTime(dailyScheduleTimeRecord, dailySchedule);
        else
            return new DailyScheduleCustomTime(dailyScheduleTimeRecord, dailySchedule);
    }
}
