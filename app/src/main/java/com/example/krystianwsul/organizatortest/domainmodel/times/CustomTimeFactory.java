package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.persistencemodel.CustomTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.Collection;
import java.util.HashMap;

public class CustomTimeFactory {
    private static CustomTimeFactory sInstance;

    public static CustomTimeFactory getInstance() {
        if (sInstance == null)
            sInstance = new CustomTimeFactory();
        return sInstance;
    }

    private HashMap<Integer, CustomTime> mCustomTimes = new HashMap<>();

    private CustomTimeFactory() {
        Collection<CustomTimeRecord> customTimeRecords = PersistenceManger.getInstance().getCustomTimeRecords();
        Assert.assertTrue(customTimeRecords != null);
        Assert.assertTrue(!customTimeRecords.isEmpty());

        for (CustomTimeRecord customTimeRecord : customTimeRecords) {
            Assert.assertTrue(customTimeRecord != null);

            CustomTime customTime = new CustomTime(customTimeRecord);
            mCustomTimes.put(customTime.getId(), customTime);
        }
    }

    public CustomTime getCustomTime(int customTimeId) {
        Assert.assertTrue(mCustomTimes.containsKey(customTimeId));
        return mCustomTimes.get(customTimeId);
    }

    public CustomTime getCustomTime(DayOfWeek dayOfWeek, HourMinute hourMinute) {
        for (CustomTime customTime : mCustomTimes.values())
            if (customTime.getHourMinute(dayOfWeek).equals(hourMinute))
                return customTime;
        return null;
    }

    public Collection<CustomTime> getCustomTimes() {
        return mCustomTimes.values();
    }
}
