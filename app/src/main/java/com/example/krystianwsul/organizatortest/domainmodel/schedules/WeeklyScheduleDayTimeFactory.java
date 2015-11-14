package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayTimeRecord;

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
        if (mWeeklyScheduleDayTimes.containsKey(weeklyScheduleDayTimeId)) {
            return mWeeklyScheduleDayTimes.get(weeklyScheduleDayTimeId);
        } else {
            WeeklyScheduleDayTime weeklyScheduleDayTime = createWeeklyScheduleDayTime(weeklyScheduleDayTimeId, weeklySchedule);
            mWeeklyScheduleDayTimes.put(weeklyScheduleDayTimeId, weeklyScheduleDayTime);
            return weeklyScheduleDayTime;
        }
    }

    private WeeklyScheduleDayTime createWeeklyScheduleDayTime(int weeklyScheduleDayTimeId, WeeklySchedule weeklySchedule) {
        WeeklyScheduleDayTimeRecord weeklyScheduleDayTimeRecord = PersistenceManger.getInstance().getWeeklyScheduleDayTimeRecord(weeklyScheduleDayTimeId);
        if (weeklyScheduleDayTimeRecord.getCustomTimeId() == null)
            return new WeeklyScheduleNormalDayTime(weeklyScheduleDayTimeId, weeklySchedule);
        else
            return new WeeklyScheduleCustomDayTime(weeklyScheduleDayTimeId, weeklySchedule);
    }
}
