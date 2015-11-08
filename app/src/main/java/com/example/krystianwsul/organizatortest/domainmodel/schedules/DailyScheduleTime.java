package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 10/29/2015.
 */
public abstract class DailyScheduleTime {
    protected final DailyScheduleTimeRecord mDailyScheduleTimeRecord;
    protected final DailySchedule mDailySchedule;

    private final static HashMap<Integer, DailyScheduleTime> sDailyScheduleTimes = new HashMap<>();

    public static DailyScheduleTime getDailyScheduleTime(int dailyScheduleTimeId, DailySchedule dailySchedule) {
        if (sDailyScheduleTimes.containsKey(dailyScheduleTimeId)) {
            return sDailyScheduleTimes.get(dailyScheduleTimeId);
        } else {
            DailyScheduleTime dailyScheduleTime = createDailyScheduleTime(dailyScheduleTimeId, dailySchedule);
            sDailyScheduleTimes.put(dailyScheduleTimeId, dailyScheduleTime);
            return dailyScheduleTime;
        }
    }

    private static DailyScheduleTime createDailyScheduleTime(int dailyScheduleTimeId, DailySchedule dailySchedule) {
        DailyScheduleTimeRecord dailyScheduleTimeRecord = PersistenceManger.getInstance().getDailyScheduleTimeRecord(dailyScheduleTimeId);
        if (dailyScheduleTimeRecord.getTimeRecordId() == null)
            return new DailyScheduleNormalTime(dailyScheduleTimeId, dailySchedule);
        else
            return new DailyScheduleCustomTime(dailyScheduleTimeId, dailySchedule);
    }

    protected DailyScheduleTime(int dailyScheduleTimeId, DailySchedule dailySchedule) {
        mDailyScheduleTimeRecord = PersistenceManger.getInstance().getDailyScheduleTimeRecord(dailyScheduleTimeId);
        Assert.assertTrue(mDailyScheduleTimeRecord != null);
        Assert.assertTrue(dailySchedule != null);
        mDailySchedule = dailySchedule;
    }

    public abstract Time getTime();

    public int getId() {
        return mDailyScheduleTimeRecord.getId();
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        return DailyRepetition.getDailyRepetition(this, scheduleDate).getInstance(task);
    }
}
