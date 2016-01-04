package com.example.krystianwsul.organizator.domainmodel.tasks;

import com.example.krystianwsul.organizator.domainmodel.dates.Date;
import com.example.krystianwsul.organizator.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizator.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizator.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizator.domainmodel.times.Time;
import com.example.krystianwsul.organizator.persistencemodel.DailyScheduleTimeRecord;

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
