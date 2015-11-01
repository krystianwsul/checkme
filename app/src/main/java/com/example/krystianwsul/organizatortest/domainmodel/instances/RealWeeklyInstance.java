package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyInstanceRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/1/2015.
 */
public class RealWeeklyInstance extends WeeklyInstance {
    private final WeeklyInstanceRecord mWeeklyInstanceRecord;

    protected RealWeeklyInstance(WeeklyInstanceRecord weeklyInstanceRecord) {
        Assert.assertTrue(weeklyInstanceRecord != null);
        mWeeklyInstanceRecord = weeklyInstanceRecord;
    }

    public  int getId() {
        return mWeeklyInstanceRecord.getId();
    }

    public int getWeeklyScheduleTimeId() {
        return mWeeklyInstanceRecord.getWeeklyScheduleTimeId();
    }

    public int getScheduleYear() {
        return mWeeklyInstanceRecord.getScheduleYear();
    }

    public int getScheduleMonth() {
        return mWeeklyInstanceRecord.getScheduleMonth();
    }

    public int getScheduleDay() {
        return mWeeklyInstanceRecord.getScheduleDay();
    }
}
