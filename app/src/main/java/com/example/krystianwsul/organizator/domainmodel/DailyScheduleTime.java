package com.example.krystianwsul.organizator.domainmodel;

import com.example.krystianwsul.organizator.persistencemodel.DailyScheduleTimeRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;

import junit.framework.Assert;

class DailyScheduleTime {
    private final DailyScheduleTimeRecord mDailyScheduleTimeRecord;

    DailyScheduleTime(DailyScheduleTimeRecord dailyScheduleTimeRecord) {
        Assert.assertTrue(dailyScheduleTimeRecord != null);
        mDailyScheduleTimeRecord = dailyScheduleTimeRecord;
    }

    public Time getTime() {
        Integer customTimeId = mDailyScheduleTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = DomainFactory.getInstance().getCustomTimeFactory().getCustomTime(mDailyScheduleTimeRecord.getCustomTimeId());
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

        return DomainFactory.getInstance().getInstanceFactory().getInstance(task, scheduleDateTime);
    }
}
