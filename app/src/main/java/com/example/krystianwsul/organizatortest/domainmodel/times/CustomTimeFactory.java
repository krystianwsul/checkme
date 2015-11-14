package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.persistencemodel.CustomTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public class CustomTimeFactory {
    private static HashMap<Integer, CustomTime> sCustomTimes = new HashMap<>();

    public static CustomTime getCustomTime(int customTimeId) {
        if (sCustomTimes.containsKey(customTimeId)) {
            return sCustomTimes.get(customTimeId);
        } else {
            CustomTimeRecord customTimeRecord = PersistenceManger.getInstance().getCustomTimeRecord(customTimeId);
            CustomTime customTime = new CustomTime(customTimeRecord);
            sCustomTimes.put(customTimeId, customTime);
            return customTime;
        }
    }

    public static CustomTime getCustomTime(DayOfWeek dayOfWeek, HourMinute hourMinute) {
        for (CustomTime customTime : sCustomTimes.values())
            if (customTime.getHourMinute(dayOfWeek).compareTo(hourMinute) == 0)
                return customTime;
        return null;
    }

    public static Collection<CustomTime> getCustomTimes() {
        return sCustomTimes.values();
    }
}
