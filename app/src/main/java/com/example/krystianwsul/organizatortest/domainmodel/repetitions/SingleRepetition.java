package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

public class SingleRepetition {
    private final SingleSchedule mSingleSchedule;

    SingleRepetition(SingleSchedule singleSchedule) {
        Assert.assertTrue(singleSchedule != null);
        mSingleSchedule = singleSchedule;
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
        return getScheduleDate();
    }

    public Time getRepetitionTime() {
        return getScheduleTime();
    }

    public DateTime getRepetitionDateTime() {
        return new DateTime(getRepetitionDate(), getRepetitionTime());
    }

    public Instance getInstance(Task task) {
        return InstanceFactory.getInstance().getSingleInstance(task, this);
    }
}
