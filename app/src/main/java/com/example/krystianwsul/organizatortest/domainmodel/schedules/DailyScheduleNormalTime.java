package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/29/2015.
 */
public class DailyScheduleNormalTime extends DailyScheduleTime {
    private final NormalTime mNormalTime;

    protected DailyScheduleNormalTime(DailyScheduleTimeRecord dailyScheduleTimeRecord, DailySchedule dailySchedule) {
        super(dailyScheduleTimeRecord, dailySchedule);
        Assert.assertTrue(mDailyScheduleTimeRecord.getCustomTimeId() == null);
        Assert.assertTrue(mDailyScheduleTimeRecord.getHour() != null);
        Assert.assertTrue(mDailyScheduleTimeRecord.getMinute() != null);

        mNormalTime = new NormalTime(mDailyScheduleTimeRecord.getHour(), mDailyScheduleTimeRecord.getMinute());
    }

    public Time getTime() {
        return mNormalTime;
    }
}
