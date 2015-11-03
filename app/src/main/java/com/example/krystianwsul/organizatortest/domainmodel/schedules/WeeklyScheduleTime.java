package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.Repetition;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleTimeRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 10/29/2015.
 */
public abstract class WeeklyScheduleTime {
    protected final WeeklyScheduleTimeRecord mWeeklyScheduleTimeRecord;

    private final static HashMap<Integer, WeeklyScheduleTime> sWeeklyScheduleTimes = new HashMap<>();

    public static WeeklyScheduleTime getWeeklyScheduleTime(int weeklyScheduleTimeId) {
        if (sWeeklyScheduleTimes.containsKey(weeklyScheduleTimeId)) {
            return sWeeklyScheduleTimes.get(weeklyScheduleTimeId);
        } else {
            WeeklyScheduleTime weeklyScheduleTime = createWeeklyScheduleTime(weeklyScheduleTimeId);
            sWeeklyScheduleTimes.put(weeklyScheduleTimeId, weeklyScheduleTime);
            return weeklyScheduleTime;
        }
    }

    private static WeeklyScheduleTime createWeeklyScheduleTime(int weeklyScheduleTimeId) {
        WeeklyScheduleTimeRecord weeklyScheduleTimeRecord = PersistenceManger.getInstance().getWeeklyScheduleTimeRecord(weeklyScheduleTimeId);
        if (weeklyScheduleTimeRecord.getTimeRecordId() == null)
            return new WeeklyScheduleNormalTime(weeklyScheduleTimeId);
        else
            return new WeeklyScheduleCustomTime(weeklyScheduleTimeId);
    }

    protected WeeklyScheduleTime(int weeklyScheduleTimeId) {
        mWeeklyScheduleTimeRecord = PersistenceManger.getInstance().getWeeklyScheduleTimeRecord(weeklyScheduleTimeId);
        Assert.assertTrue(mWeeklyScheduleTimeRecord != null);
    }

    public abstract Time getTime();

    public int getId() {
        return mWeeklyScheduleTimeRecord.getId();
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        return WeeklyRepetition.getWeeklyRepetition(this, scheduleDate).getInstance(task);
    }
}
