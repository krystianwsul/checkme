package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/1/2015.
 */
public class VirtualDailyRepetition extends DailyRepetition {
    private final DailyScheduleTime mDailyScheduleTime;

    private final int mId;

    private final int mScheduleYear;
    private final int mScheduleMonth;
    private final int mScheduleDay;

    private static int sRepetitionCount = 0;

    public VirtualDailyRepetition(DailyScheduleTime dailyScheduleTime, Date scheduleDate) {
        Assert.assertTrue(dailyScheduleTime != null);

        mDailyScheduleTime = dailyScheduleTime;

        mScheduleYear = scheduleDate.getYear();
        mScheduleMonth = scheduleDate.getMonth();
        mScheduleDay = scheduleDate.getDay();

        sRepetitionCount++;

        mId = PersistenceManger.getInstance().getMaxDailyRepetitionId() + sRepetitionCount;
    }

    public int getId() {
        return mId;
    }

    public int getDailyScheduleTimeId() {
        return mDailyScheduleTime.getId();
    }

    public int getScheduleYear() {
        return mScheduleYear;
    }

    public int getScheduleMonth() {
        return mScheduleMonth;
    }

    public int getScheduleDay() {
        return mScheduleDay;
    }

    public Integer getRepetitionYear() {
        return null;
    }

    public Integer getRepetitionMonth() {
        return null;
    }

    public Integer getRepetitionDay() {
        return null;
    }

    public Date getDate() {
        return new Date(mScheduleYear, mScheduleMonth, mScheduleDay);
    }

    public Time getTime() {
        return mDailyScheduleTime.getTime();
    }
}
