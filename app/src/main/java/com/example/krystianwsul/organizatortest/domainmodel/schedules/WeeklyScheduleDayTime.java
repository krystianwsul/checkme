package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
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

    private final static HashMap<Integer, WeeklyScheduleDayTime> sWeeklyScheduleDayTimes = new HashMap<>();

    public static WeeklyScheduleDayTime getWeeklyScheduleDayTime(int weeklyScheduleDayTimeId, WeeklySchedule weeklySchedule) {
        if (sWeeklyScheduleDayTimes.containsKey(weeklyScheduleDayTimeId)) {
            return sWeeklyScheduleDayTimes.get(weeklyScheduleDayTimeId);
        } else {
            WeeklyScheduleDayTime weeklyScheduleDayTime = createWeeklyScheduleDayTime(weeklyScheduleDayTimeId, weeklySchedule);
            sWeeklyScheduleDayTimes.put(weeklyScheduleDayTimeId, weeklyScheduleDayTime);
            return weeklyScheduleDayTime;
        }
    }

    private static WeeklyScheduleDayTime createWeeklyScheduleDayTime(int weeklyScheduleDayTimeId, WeeklySchedule weeklySchedule) {
        WeeklyScheduleDayTimeRecord weeklyScheduleDayTimeRecord = PersistenceManger.getInstance().getWeeklyScheduleDayTimeRecord(weeklyScheduleDayTimeId);
        if (weeklyScheduleDayTimeRecord.getTimeRecordId() == null)
            return new WeeklyScheduleNormalDayTime(weeklyScheduleDayTimeId, weeklySchedule);
        else
            return new WeeklyScheduleCustomDayTime(weeklyScheduleDayTimeId, weeklySchedule);
    }

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
        return WeeklyRepetition.getWeeklyRepetition(this, scheduleDate).getInstance(task);
    }
}
