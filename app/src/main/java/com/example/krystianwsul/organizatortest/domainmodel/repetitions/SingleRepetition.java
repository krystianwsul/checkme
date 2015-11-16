package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.SingleInstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleRepetitionRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/14/2015.
 */
public class SingleRepetition {
    private final SingleSchedule mSingleSchedule;
    private final SingleRepetitionRecord mSingleRepetitionRecord;

    SingleRepetition(SingleSchedule singleSchedule, SingleRepetitionRecord singleRepetitionRecord) {
        Assert.assertTrue(singleSchedule != null);
        Assert.assertTrue(singleRepetitionRecord != null);

        mSingleSchedule = singleSchedule;
        mSingleRepetitionRecord = singleRepetitionRecord;
    }

    SingleRepetition(SingleSchedule singleSchedule) {
        Assert.assertTrue(singleSchedule != null);

        mSingleSchedule = singleSchedule;
        mSingleRepetitionRecord = null;
    }

    public int getRootTaskId() {
        return mSingleSchedule.getRootTaskId();
    }

    public Date getScheduleDate() {
        return mSingleSchedule.getDate();
    }

    public Time getScheduleTime() {
        return mSingleSchedule.getTime();
    }

    public DateTime getScheduleDateTime() {
        return mSingleSchedule.getDateTime();
    }

    public Date getRepetitionDate() {
        if (mSingleRepetitionRecord != null && mSingleRepetitionRecord.getRepetitionYear() != null)
            return new Date(mSingleRepetitionRecord.getRepetitionYear(), mSingleRepetitionRecord.getRepetitionMonth(), mSingleRepetitionRecord.getRepetitionDay());
        else
            return getScheduleDate();
    }

    public Time getRepetitionTime() {
        if (mSingleRepetitionRecord != null) {
            if (mSingleRepetitionRecord.getCustomTimeId() != null) {
                return CustomTimeFactory.getInstance().getCustomTime(mSingleRepetitionRecord.getCustomTimeId());
            } else {
                Assert.assertTrue(mSingleRepetitionRecord.getHour() != null);
                return new NormalTime(mSingleRepetitionRecord.getHour(), mSingleRepetitionRecord.getMinute());
            }
        } else {
            return getScheduleTime();
        }
    }

    public DateTime getRepetitionDateTime() {
        return new DateTime(getRepetitionDate(), getRepetitionTime());
    }

    public Instance getInstance(Task task) {
        return SingleInstanceFactory.getInstance().getSingleInstance(task, this);
    }
}
