package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyScheduleNormalDayTime extends WeeklyScheduleDayTime {
    private final NormalTime mNormalTime;

    protected WeeklyScheduleNormalDayTime(int weeklyScheduleDayTimeId, WeeklySchedule weeklySchedule) {
        super(weeklyScheduleDayTimeId, weeklySchedule);
        Assert.assertTrue(mWeeklyScheduleDayTimeRecord.getTimeRecordId() == null);
        Assert.assertTrue(mWeeklyScheduleDayTimeRecord.getHour() != null);
        Assert.assertTrue(mWeeklyScheduleDayTimeRecord.getMinute() != null);

        mNormalTime = new NormalTime(mWeeklyScheduleDayTimeRecord.getHour(), mWeeklyScheduleDayTimeRecord.getMinute());
    }

    public Time getTime() {
        return mNormalTime;
    }
}
