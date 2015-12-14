package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;

import junit.framework.Assert;

public class DailyScheduleTime {
    private final DailyScheduleTimeRecord mDailyScheduleTimeRecord;
    private final DailySchedule mDailySchedule;

    DailyScheduleTime(DailyScheduleTimeRecord dailyScheduleTimeRecord, DailySchedule dailySchedule) {
        Assert.assertTrue(dailyScheduleTimeRecord != null);
        Assert.assertTrue(dailySchedule != null);

        mDailyScheduleTimeRecord = dailyScheduleTimeRecord;
        mDailySchedule = dailySchedule;
    }

    public Time getTime() {
        Integer customTimeId = mDailyScheduleTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = CustomTimeFactory.getInstance().getCustomTime(mDailyScheduleTimeRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);
            return customTime;
        } else {
            Integer hour = mDailyScheduleTimeRecord.getHour();
            Integer minute = mDailyScheduleTimeRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    public int getId() {
        return mDailyScheduleTimeRecord.getId();
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDate != null);

        DateTime scheduleDateTime = new DateTime(scheduleDate, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp()));

        return InstanceFactory.getInstance().getInstance(task, scheduleDateTime);
    }
}
