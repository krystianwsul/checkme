package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/29/2015.
 */
public class WeeklyScheduleNormalTime extends WeeklyScheduleTime {
    private final NormalTime mNormalTime;

    protected WeeklyScheduleNormalTime(int weeklyScheduleTimeId) {
        super(weeklyScheduleTimeId);
        Assert.assertTrue(mWeeklyScheduleTimeRecord.getTimeRecordId() == null);
        Assert.assertTrue(mWeeklyScheduleTimeRecord.getHour() != null);
        Assert.assertTrue(mWeeklyScheduleTimeRecord.getMinute() != null);

        mNormalTime = new NormalTime(mWeeklyScheduleTimeRecord.getHour(), mWeeklyScheduleTimeRecord.getMinute());
    }

    public Time getTime() {
        return mNormalTime;
    }
}
