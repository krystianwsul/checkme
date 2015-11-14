package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayTimeRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyScheduleCustomDayTime extends WeeklyScheduleDayTime {
    private final CustomTime mCustomTime;

    protected WeeklyScheduleCustomDayTime(WeeklyScheduleDayTimeRecord weeklyScheduleDayTimeRecord, WeeklySchedule weeklySchedule) {
        super(weeklyScheduleDayTimeRecord, weeklySchedule);
        Assert.assertTrue(mWeeklyScheduleDayTimeRecord.getCustomTimeId() != null);
        Assert.assertTrue(mWeeklyScheduleDayTimeRecord.getHour() == null);
        Assert.assertTrue(mWeeklyScheduleDayTimeRecord.getMinute() == null);

        mCustomTime = CustomTimeFactory.getCustomTime(mWeeklyScheduleDayTimeRecord.getCustomTimeId());
        Assert.assertTrue(mCustomTime != null);
    }

    public Time getTime() {
        return mCustomTime;
    }
}
