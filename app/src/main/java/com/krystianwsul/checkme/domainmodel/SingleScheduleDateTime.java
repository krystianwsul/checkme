package com.krystianwsul.checkme.domainmodel;

import com.krystianwsul.checkme.persistencemodel.SingleScheduleDateTimeRecord;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

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

            CustomTime customTime = domainFactory.getCustomTime(mSingleScheduleDateTimeRecord.getCustomTimeId());
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

    private Date getDate() {
        return new Date(mSingleScheduleDateTimeRecord.getYear(), mSingleScheduleDateTimeRecord.getMonth(), mSingleScheduleDateTimeRecord.getDay());
    }

    DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    Instance getInstance(Task task) {
        Assert.assertTrue(task != null);

        DateTime scheduleDateTime = getDateTime();
        //Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp())); zone hack

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getInstance(task, scheduleDateTime);
   }
}
