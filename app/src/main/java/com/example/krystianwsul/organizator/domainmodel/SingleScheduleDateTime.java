package com.example.krystianwsul.organizator.domainmodel;

import com.example.krystianwsul.organizator.persistencemodel.SingleScheduleDateTimeRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;

import junit.framework.Assert;

class SingleScheduleDateTime {
    private final DomainFactory mDomainFactory;

    private final SingleScheduleDateTimeRecord mSingleScheduleDateTimeRecord;

    SingleScheduleDateTime(DomainFactory domainFactory, SingleScheduleDateTimeRecord singleScheduleDateTimeRecord) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        mDomainFactory = domainFactory;
        mSingleScheduleDateTimeRecord = singleScheduleDateTimeRecord;
    }

    public Time getTime() {
        Integer customTimeId = mSingleScheduleDateTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = mDomainFactory.getCustomTimeFactory().getCustomTime(mSingleScheduleDateTimeRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);
            return customTime;
        } else {
            Integer hour = mSingleScheduleDateTimeRecord.getHour();
            Integer minute = mSingleScheduleDateTimeRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    public Date getDate() {
        return new Date(mSingleScheduleDateTimeRecord.getYear(), mSingleScheduleDateTimeRecord.getMonth(), mSingleScheduleDateTimeRecord.getDay());
    }

    public DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDate != null);

        DateTime scheduleDateTime = new DateTime(scheduleDate, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp()));

        return mDomainFactory.getInstanceFactory().getInstance(task, scheduleDateTime);
   }
}
