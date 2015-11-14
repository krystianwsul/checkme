package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.WeeklyInstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleDayTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public abstract class WeeklyRepetition {
    protected final WeeklyScheduleDayTime mWeeklyScheduleDayTime;

    protected WeeklyRepetition(WeeklyScheduleDayTime weeklyScheduleDayTime) {
        Assert.assertTrue(weeklyScheduleDayTime != null);
        mWeeklyScheduleDayTime = weeklyScheduleDayTime;
    }

    public abstract int getId();

    public abstract int getWeeklyScheduleDayTimeId();

    public abstract Date getScheduleDate();

    public abstract Time getScheduleTime();

    public DateTime getScheduleDateTime() {
        return new DateTime(getScheduleDate(), getScheduleTime());
    }

    public abstract Date getRepetitionDate();

    public abstract Time getRepetitionTime();

    public DateTime getRepetitionDateTime() {
        return new DateTime(getRepetitionDate(), getRepetitionTime());
    }

    public Instance getInstance(Task task) {
        return WeeklyInstanceFactory.getInstance().getWeeklyInstance(task, this);
    }
}
