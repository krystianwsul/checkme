package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleTime;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/1/2015.
 */
public class VirtualWeeklyRepetition extends WeeklyRepetition {
    private final WeeklyScheduleTime mWeeklyScheduleTime;

    private final int mId;

    private final int mScheduleYear;
    private final int mScheduleMonth;
    private final int mScheduleDay;

    private static int sRepetitionCount = 0;

    public VirtualWeeklyRepetition(WeeklyScheduleTime weeklyScheduleTime, Date scheduleDate) {
        Assert.assertTrue(weeklyScheduleTime != null);

        mWeeklyScheduleTime = weeklyScheduleTime;

        mScheduleYear = scheduleDate.getYear();
        mScheduleMonth = scheduleDate.getMonth();
        mScheduleDay = scheduleDate.getDay();

        sRepetitionCount++;

        mId = PersistenceManger.getInstance().getMaxWeeklyRepetitionId() + sRepetitionCount;
    }

    public int getId() {
        return mId;
    }

    public int getWeeklyScheduleTimeId() {
        return mWeeklyScheduleTime.getId();
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
}
