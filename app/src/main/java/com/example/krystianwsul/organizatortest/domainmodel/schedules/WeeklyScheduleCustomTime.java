package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/29/2015.
 */
public class WeeklyScheduleCustomTime extends WeeklyScheduleTime {
    private final CustomTime mCustomTime;

    protected WeeklyScheduleCustomTime(int weeklyScheduleTimeId) {
        super(weeklyScheduleTimeId);
        Assert.assertTrue(mWeeklyScheduleTimeRecord.getTimeRecordId() != null);
        Assert.assertTrue(mWeeklyScheduleTimeRecord.getHour() == null);
        Assert.assertTrue(mWeeklyScheduleTimeRecord.getMinute() == null);

        mCustomTime = CustomTime.getCustomTime(mWeeklyScheduleTimeRecord.getTimeRecordId());
        Assert.assertTrue(mCustomTime != null);
    }

    public Time getTime() {
        return mCustomTime;
    }
}
