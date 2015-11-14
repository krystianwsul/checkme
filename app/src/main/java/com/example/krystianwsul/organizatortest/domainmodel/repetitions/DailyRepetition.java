package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
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

    private static final HashMap<Integer, DailyRepetition> sDailyRepetitions = new HashMap<>();

    public static DailyRepetition getDailyRepetition(int dailyRepetitionId, DailyScheduleTime dailyScheduleTime) {
        if (sDailyRepetitions.containsKey(dailyRepetitionId)) {
            return sDailyRepetitions.get(dailyRepetitionId);
        } else {
            DailyRepetition dailyRepetition = new RealDailyRepetition(PersistenceManger.getInstance().getDailyRepetitionRecord(dailyRepetitionId), dailyScheduleTime);
            sDailyRepetitions.put(dailyRepetitionId, dailyRepetition);
            return dailyRepetition;
        }
    }

    public static DailyRepetition getDailyRepetition(DailyScheduleTime dailyScheduleTime, Date scheduleDate) {
        DailyRepetition existingDailyRepetition = getExistingDailyRepetition(dailyScheduleTime.getId(), scheduleDate);
        if (existingDailyRepetition != null)
            return existingDailyRepetition;

        DailyRepetitionRecord dailyRepetitionRecord = PersistenceManger.getInstance().getDailyRepetitionRecord(dailyScheduleTime.getId(), scheduleDate);
        if (dailyRepetitionRecord != null) {
            RealDailyRepetition realDailyRepetition = new RealDailyRepetition(dailyRepetitionRecord, dailyScheduleTime);
            sDailyRepetitions.put(realDailyRepetition.getId(), realDailyRepetition);
            return realDailyRepetition;
        }

        VirtualDailyRepetition virtualDailyRepetition = new VirtualDailyRepetition(dailyScheduleTime, scheduleDate);
        sDailyRepetitions.put(virtualDailyRepetition.getId(), virtualDailyRepetition);
        return virtualDailyRepetition;
    }

    private static DailyRepetition getExistingDailyRepetition(int dailyScheduleTimeId, Date scheduleDate) {
        for (DailyRepetition dailyRepetition : sDailyRepetitions.values())
            if (dailyRepetition.getDailyScheduleTimeId() == dailyScheduleTimeId && dailyRepetition.getScheduleDate() == scheduleDate)
                return dailyRepetition;
        return null;
    }

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
        return DailyInstance.getDailyInstance(task, this);
    }
}
