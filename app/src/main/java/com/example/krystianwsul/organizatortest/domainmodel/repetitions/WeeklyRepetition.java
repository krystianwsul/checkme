package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.DailyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.WeeklyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleDayTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyRepetitionRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/9/2015.
 */
public abstract class WeeklyRepetition {
    protected final WeeklyScheduleDayTime mWeeklyScheduleDayTime;

    private static final HashMap<Integer, WeeklyRepetition> sWeeklyRepetitions = new HashMap<>();

    public static WeeklyRepetition getWeeklyRepetition(int weeklyRepetitionId, WeeklyScheduleDayTime weeklyScheduleDayTime) {
        if (sWeeklyRepetitions.containsKey(weeklyRepetitionId)) {
            return sWeeklyRepetitions.get(weeklyRepetitionId);
        } else {
            WeeklyRepetition weeklyRepetition = new RealWeeklyRepetition(PersistenceManger.getInstance().getWeeklyRepetitionRecord(weeklyRepetitionId), weeklyScheduleDayTime);
            sWeeklyRepetitions.put(weeklyRepetitionId, weeklyRepetition);
            return weeklyRepetition;
        }
    }

    public static WeeklyRepetition getWeeklyRepetition(WeeklyScheduleDayTime weeklyScheduleDayTime, Date scheduleDate) {
        WeeklyRepetition existingWeeklyRepetition = getExistingWeeklyRepetition(weeklyScheduleDayTime.getId(), scheduleDate);
        if (existingWeeklyRepetition != null)
            return existingWeeklyRepetition;

        WeeklyRepetitionRecord weeklyRepetitionRecord = PersistenceManger.getInstance().getWeeklyRepetitionRecord(weeklyScheduleDayTime.getId(), scheduleDate);
        if (weeklyRepetitionRecord != null) {
            RealWeeklyRepetition realWeeklyRepetition = new RealWeeklyRepetition(weeklyRepetitionRecord, weeklyScheduleDayTime);
            sWeeklyRepetitions.put(realWeeklyRepetition.getId(), realWeeklyRepetition);
            return realWeeklyRepetition;
        }

        VirtualWeeklyRepetition virtualWeeklyRepetition = new VirtualWeeklyRepetition(weeklyScheduleDayTime, scheduleDate);
        sWeeklyRepetitions.put(virtualWeeklyRepetition.getId(), virtualWeeklyRepetition);
        return virtualWeeklyRepetition;
    }

    private static WeeklyRepetition getExistingWeeklyRepetition(int weeklyScheduleDayTimeId, Date scheduleDate) {
        for (WeeklyRepetition weeklyRepetition : sWeeklyRepetitions.values())
            if (weeklyRepetition.getWeeklyScheduleDayTimeId() == weeklyScheduleDayTimeId && weeklyRepetition.getScheduleDate() == scheduleDate)
                return weeklyRepetition;
        return null;
    }

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
        return WeeklyInstance.getWeeklyInstance(task, this);
    }
}
