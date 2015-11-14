package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayTimeRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public abstract class WeeklyScheduleDayTime {
    protected final WeeklyScheduleDayTimeRecord mWeeklyScheduleDayTimeRecord;
    protected final WeeklySchedule mWeeklySchedule;

    protected WeeklyScheduleDayTime(WeeklyScheduleDayTimeRecord weeklyScheduleDayTimeRecord, WeeklySchedule weeklySchedule) {
        Assert.assertTrue(weeklyScheduleDayTimeRecord != null);
        Assert.assertTrue(weeklySchedule != null);

        mWeeklyScheduleDayTimeRecord = weeklyScheduleDayTimeRecord;
        mWeeklySchedule = weeklySchedule;
    }

    public abstract Time getTime();

    public int getId() {
        return mWeeklyScheduleDayTimeRecord.getId();
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.values()[mWeeklyScheduleDayTimeRecord.getDayOfWeek()];
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        return WeeklyRepetitionFactory.getInstance().getWeeklyRepetition(this, scheduleDate).getInstance(task);
    }
}
