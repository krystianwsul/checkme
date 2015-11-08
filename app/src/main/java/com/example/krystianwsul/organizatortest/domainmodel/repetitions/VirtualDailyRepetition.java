package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailySchedule;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/1/2015.
 */
public class VirtualDailyRepetition extends DailyRepetition {
    private final int mId;

    private final Date mScheduleDate;

    private static int sRepetitionCount = 0;

    public VirtualDailyRepetition(DailyScheduleTime dailyScheduleTime, Date scheduleDate) {
        super(dailyScheduleTime);

        Assert.assertTrue(scheduleDate != null);
        mScheduleDate = scheduleDate;

        sRepetitionCount++;

        mId = PersistenceManger.getInstance().getMaxDailyRepetitionId() + sRepetitionCount;
    }

    public int getId() {
        return mId;
    }

    public int getDailyScheduleTimeId() {
        return mDailyScheduleTime.getId();
    }

    public Date getScheduleDate() {
        return mScheduleDate;
    }

    public Time getScheduleTime() {
        return mDailyScheduleTime.getTime();
    }

    public Date getRepetitionDate() {
        return getScheduleDate();
    }

    public Time getRepetitionTime() {
        return getScheduleTime();
    }
}
