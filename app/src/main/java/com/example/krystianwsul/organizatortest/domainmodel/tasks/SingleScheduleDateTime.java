package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleScheduleDateTimeRecord;

import junit.framework.Assert;

public class SingleScheduleDateTime {
    private final SingleScheduleDateTimeRecord mSingleScheduleDateTimeRecord;
    private final SingleSchedule mSingleSchedule;

    SingleScheduleDateTime(SingleScheduleDateTimeRecord singleScheduleDateTimeRecord, SingleSchedule singleSchedule) {
        Assert.assertTrue(singleScheduleDateTimeRecord != null);
        Assert.assertTrue(singleSchedule != null);

        mSingleScheduleDateTimeRecord = singleScheduleDateTimeRecord;
        mSingleSchedule = singleSchedule;
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

    public int getRootTaskId() {
        return mSingleSchedule.getRootTaskId();
    }

    public Date getDate() {
        return new Date(mSingleScheduleDateTimeRecord.getYear(), mSingleScheduleDateTimeRecord.getMonth(), mSingleScheduleDateTimeRecord.getDay());
    }

    public DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        return SingleRepetitionFactory.getInstance().getSingleRepetition(this).getInstance(task);
    }
}
