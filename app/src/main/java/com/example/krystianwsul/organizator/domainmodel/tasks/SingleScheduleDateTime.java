package com.example.krystianwsul.organizator.domainmodel.tasks;

import com.example.krystianwsul.organizator.domainmodel.dates.Date;
import com.example.krystianwsul.organizator.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizator.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizator.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizator.domainmodel.times.Time;
import com.example.krystianwsul.organizator.persistencemodel.SingleScheduleDateTimeRecord;

import junit.framework.Assert;

class SingleScheduleDateTime {
    private final SingleScheduleDateTimeRecord mSingleScheduleDateTimeRecord;

    SingleScheduleDateTime(SingleScheduleDateTimeRecord singleScheduleDateTimeRecord) {
        Assert.assertTrue(singleScheduleDateTimeRecord != null);
        mSingleScheduleDateTimeRecord = singleScheduleDateTimeRecord;
    }

    public Time getTime() {
        Integer customTimeId = mSingleScheduleDateTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = CustomTimeFactory.getInstance().getCustomTime(mSingleScheduleDateTimeRecord.getCustomTimeId());
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

        return InstanceFactory.getInstance().getInstance(task, scheduleDateTime);
   }
}
