package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayTimeRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/9/2015.
 */
public abstract class WeeklyScheduleDayTime {
    protected final WeeklyScheduleDayTimeRecord mWeeklyScheduleDayTimeRecord;
    protected final WeeklySchedule mWeeklySchedule;

    protected WeeklyScheduleDayTime(int weeklyScheduleDayTimeId, WeeklySchedule weeklySchedule) {
        mWeeklyScheduleDayTimeRecord = PersistenceManger.getInstance().getWeeklyScheduleDayTimeRecord(weeklyScheduleDayTimeId);
        Assert.assertTrue(mWeeklyScheduleDayTimeRecord != null);
        Assert.assertTrue(weeklySchedule != null);
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
