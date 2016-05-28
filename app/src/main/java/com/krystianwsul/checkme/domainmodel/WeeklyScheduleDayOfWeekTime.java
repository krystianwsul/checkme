package com.krystianwsul.checkme.domainmodel;

import com.krystianwsul.checkme.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

import java.lang.ref.WeakReference;

class WeeklyScheduleDayOfWeekTime {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final WeeklyScheduleDayOfWeekTimeRecord mWeeklyScheduleDayOfWeekTimeRecord;

    WeeklyScheduleDayOfWeekTime(DomainFactory domainFactory, WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecord != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);
        mWeeklyScheduleDayOfWeekTimeRecord = weeklyScheduleDayOfWeekTimeRecord;
    }

    Time getTime() {
        Integer customTimeId = mWeeklyScheduleDayOfWeekTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            DomainFactory domainFactory = mDomainFactoryReference.get();
            Assert.assertTrue(domainFactory != null);

            CustomTime customTime = domainFactory.getCustomTime(mWeeklyScheduleDayOfWeekTimeRecord.getCustomTimeId());
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

    DayOfWeek getDayOfWeek() {
        return DayOfWeek.values()[mWeeklyScheduleDayOfWeekTimeRecord.getDayOfWeek()];
    }

    Instance getInstance(Task task, Date scheduleDate) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDate != null);

        DateTime scheduleDateTime = new DateTime(scheduleDate, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp()));

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getInstance(task, scheduleDateTime);
    }
}
