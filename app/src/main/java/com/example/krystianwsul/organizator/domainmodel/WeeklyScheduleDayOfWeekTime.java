package com.example.krystianwsul.organizator.domainmodel;

import com.example.krystianwsul.organizator.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;

import junit.framework.Assert;

class WeeklyScheduleDayOfWeekTime {
    private final DomainFactory mDomainFactory;

    private final WeeklyScheduleDayOfWeekTimeRecord mWeeklyScheduleDayOfWeekTimeRecord;

    WeeklyScheduleDayOfWeekTime(DomainFactory domainFactory, WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecord != null);

        mDomainFactory = domainFactory;
        mWeeklyScheduleDayOfWeekTimeRecord = weeklyScheduleDayOfWeekTimeRecord;
    }

    public Time getTime() {
        Integer customTimeId = mWeeklyScheduleDayOfWeekTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = mDomainFactory.getCustomTimeFactory().getCustomTime(mWeeklyScheduleDayOfWeekTimeRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);
            return customTime;
        } else {
            Integer hour = mWeeklyScheduleDayOfWeekTimeRecord.getHour();
            Integer minute = mWeeklyScheduleDayOfWeekTimeRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    public int getId() {
        return mWeeklyScheduleDayOfWeekTimeRecord.getId();
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.values()[mWeeklyScheduleDayOfWeekTimeRecord.getDayOfWeek()];
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDate != null);

        DateTime scheduleDateTime = new DateTime(scheduleDate, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp()));

        return mDomainFactory.getInstanceFactory().getInstance(task, scheduleDateTime);
    }
}
