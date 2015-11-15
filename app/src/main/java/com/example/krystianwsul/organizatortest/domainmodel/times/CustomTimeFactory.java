package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.persistencemodel.CustomTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public class CustomTimeFactory {
    private static CustomTimeFactory sInstance;

    public static CustomTimeFactory getInstance() {
        if (sInstance == null)
            sInstance = new CustomTimeFactory();
        return sInstance;
    }

    private HashMap<Integer, CustomTime> mCustomTimes = new HashMap<>();

    public CustomTime getCustomTime(int customTimeId) {
        if (mCustomTimes.containsKey(customTimeId)) {
            return mCustomTimes.get(customTimeId);
        } else {
            CustomTimeRecord customTimeRecord = PersistenceManger.getInstance().getCustomTimeRecord(customTimeId);
            CustomTime customTime = new CustomTime(customTimeRecord);
            mCustomTimes.put(customTimeId, customTime);
            return customTime;
        }
    }

    public CustomTime getCustomTime(DayOfWeek dayOfWeek, HourMinute hourMinute) {
        for (CustomTime customTime : mCustomTimes.values())
            if (customTime.getHourMinute(dayOfWeek).compareTo(hourMinute) == 0)
                return customTime;
        return null;
    }

    public Collection<CustomTime> getCustomTimes() {
        return mCustomTimes.values();
    }
}
