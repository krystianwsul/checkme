package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.DailyInstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.DailyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 10/31/2015.
 */
public abstract class DailyRepetition {
    protected final DailyScheduleTime mDailyScheduleTime;

    protected DailyRepetition(DailyScheduleTime dailyScheduleTime) {
        Assert.assertTrue(dailyScheduleTime != null);
        mDailyScheduleTime = dailyScheduleTime;
    }

    public abstract int getId();

    public abstract int getDailyScheduleTimeId();

    public abstract Date getScheduleDate();

    public Time getScheduleTime() {
        return mDailyScheduleTime.getTime();
    }

    public DateTime getScheduleDateTime() {
        return new DateTime(getScheduleDate(), getScheduleTime());
    }

    public abstract Date getRepetitionDate();

    public abstract Time getRepetitionTime();

    public DateTime getRepetitionDateTime() {
        return new DateTime(getRepetitionDate(), getRepetitionTime());
    }

    public Instance getInstance(Task task) {
        return DailyInstanceFactory.getInstance().getDailyInstance(task, this);
    }
}
