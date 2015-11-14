package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/29/2015.
 */
public class DailyScheduleCustomTime extends DailyScheduleTime {
    private final CustomTime mCustomTime;

    protected DailyScheduleCustomTime(int dailyScheduleTimeId, DailySchedule dailySchedule) {
        super(dailyScheduleTimeId, dailySchedule);
        Assert.assertTrue(mDailyScheduleTimeRecord.getCustomTimeId() != null);
        Assert.assertTrue(mDailyScheduleTimeRecord.getHour() == null);
        Assert.assertTrue(mDailyScheduleTimeRecord.getMinute() == null);

        mCustomTime = CustomTimeFactory.getCustomTime(mDailyScheduleTimeRecord.getCustomTimeId());
        Assert.assertTrue(mCustomTime != null);
    }

    public Time getTime() {
        return mCustomTime;
    }
}
