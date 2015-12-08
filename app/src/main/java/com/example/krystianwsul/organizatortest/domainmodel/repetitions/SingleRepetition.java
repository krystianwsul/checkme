package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.SingleScheduleDateTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

public class SingleRepetition {
    private final SingleScheduleDateTime mSingleScheduleDateTime;

    SingleRepetition(SingleScheduleDateTime singleSchedulDateTime) {
        Assert.assertTrue(singleSchedulDateTime != null);
        mSingleScheduleDateTime = singleSchedulDateTime;
    }

    public int getRootTaskId() {
        return mSingleScheduleDateTime.getRootTaskId();
    }

    public Date getScheduleDate() {
        return mSingleScheduleDateTime.getDate();
    }

    public Time getScheduleTime() {
        return mSingleScheduleDateTime.getTime();
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
