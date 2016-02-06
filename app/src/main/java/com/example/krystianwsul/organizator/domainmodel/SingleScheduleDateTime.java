package com.example.krystianwsul.organizator.domainmodel;

import com.example.krystianwsul.organizator.persistencemodel.SingleScheduleDateTimeRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;

import junit.framework.Assert;

import java.lang.ref.WeakReference;

class SingleScheduleDateTime {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final SingleScheduleDateTimeRecord mSingleScheduleDateTimeRecord;

    SingleScheduleDateTime(DomainFactory domainFactory, SingleScheduleDateTimeRecord singleScheduleDateTimeRecord) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);
        mSingleScheduleDateTimeRecord = singleScheduleDateTimeRecord;
    }

    Time getTime() {
        Integer customTimeId = mSingleScheduleDateTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            DomainFactory domainFactory = mDomainFactoryReference.get();
            Assert.assertTrue(domainFactory != null);

            CustomTime customTime = domainFactory.getCustomTimeFactory().getCustomTime(mSingleScheduleDateTimeRecord.getCustomTimeId());
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

    Date getDate() {
        return new Date(mSingleScheduleDateTimeRecord.getYear(), mSingleScheduleDateTimeRecord.getMonth(), mSingleScheduleDateTimeRecord.getDay());
    }

    DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    Instance getInstance(Task task, Date scheduleDate) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDate != null);

        DateTime scheduleDateTime = new DateTime(scheduleDate, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp()));

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getInstanceFactory().getInstance(task, scheduleDateTime);
   }
}
