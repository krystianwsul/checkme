package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleDayTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public class VirtualWeeklyRepetition extends WeeklyRepetition {
    private final int mId;

    private final Date mScheduleDate;

    private static int sRepetitionCount = 0;

    public VirtualWeeklyRepetition(WeeklyScheduleDayTime weeklyScheduleDayTime, Date scheduleDate) {
        super(weeklyScheduleDayTime);

        Assert.assertTrue(scheduleDate != null);
        mScheduleDate = scheduleDate;

        sRepetitionCount++;

        mId = PersistenceManger.getInstance().getMaxWeeklyRepetitionId() + sRepetitionCount;
    }

    public int getId() {
        return mId;
    }

    public int getWeeklyScheduleDayTimeId() {
        return mWeeklyScheduleDayTime.getId();
    }

    public Date getScheduleDate() {
        return mScheduleDate;
    }

    public Time getScheduleTime() {
        return mWeeklyScheduleDayTime.getTime();
    }

    public Date getRepetitionDate() {
        return getScheduleDate();
    }

    public Time getRepetitionTime() {
        return getScheduleTime();
    }
}
