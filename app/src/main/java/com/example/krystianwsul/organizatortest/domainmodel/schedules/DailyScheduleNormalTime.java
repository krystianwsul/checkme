package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/29/2015.
 */
public class DailyScheduleNormalTime extends DailyScheduleTime {
    private final NormalTime mNormalTime;

    protected DailyScheduleNormalTime(int dailyScheduleTimeId, DailySchedule dailySchedule) {
        super(dailyScheduleTimeId, dailySchedule);
        Assert.assertTrue(mDailyScheduleTimeRecord.getCustomTimeId() == null);
        Assert.assertTrue(mDailyScheduleTimeRecord.getHour() != null);
        Assert.assertTrue(mDailyScheduleTimeRecord.getMinute() != null);

        mNormalTime = new NormalTime(mDailyScheduleTimeRecord.getHour(), mDailyScheduleTimeRecord.getMinute());
    }

    public Time getTime() {
        return mNormalTime;
    }
}
