package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleTime;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/1/2015.
 */
public class VirtualWeeklyInstance extends WeeklyInstance {
    private final WeeklyScheduleTime mWeeklyScheduleTime;

    private final int mId;

    private final int mScheduleYear;
    private final int mScheduleMonth;
    private final int mScheduleDay;

    private static int sInstanceCount = 0;

    public VirtualWeeklyInstance(WeeklyScheduleTime weeklyScheduleTime, Date scheduleDate) {
        Assert.assertTrue(weeklyScheduleTime != null);

        mWeeklyScheduleTime = weeklyScheduleTime;

        mScheduleYear = scheduleDate.getYear();
        mScheduleMonth = scheduleDate.getMonth();
        mScheduleDay = scheduleDate.getDay();

        sInstanceCount++;

        mId = PersistenceManger.getInstance().getMaxWeeklyInstanceId() + sInstanceCount;
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
