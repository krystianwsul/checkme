package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/29/2015.
 */
public class DailyScheduleCustomTime extends DailyScheduleTime {
    private final CustomTime mCustomTime;

    protected DailyScheduleCustomTime(DailyScheduleTimeRecord dailyScheduleTimeRecord, DailySchedule dailySchedule) {
        super(dailyScheduleTimeRecord, dailySchedule);
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
