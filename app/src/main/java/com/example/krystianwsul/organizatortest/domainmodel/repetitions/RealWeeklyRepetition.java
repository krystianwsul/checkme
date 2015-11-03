package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyRepetitionRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/1/2015.
 */
public class RealWeeklyRepetition extends WeeklyRepetition {
    private final WeeklyRepetitionRecord mWeeklyRepetitionRecord;

    protected RealWeeklyRepetition(WeeklyRepetitionRecord weeklyRepetitionRecord) {
        Assert.assertTrue(weeklyRepetitionRecord != null);
        mWeeklyRepetitionRecord = weeklyRepetitionRecord;
    }

    public  int getId() {
        return mWeeklyRepetitionRecord.getId();
    }

    public int getWeeklyScheduleTimeId() {
        return mWeeklyRepetitionRecord.getWeeklyScheduleTimeId();
    }

    public int getScheduleYear() {
        return mWeeklyRepetitionRecord.getScheduleYear();
    }

    public int getScheduleMonth() {
        return mWeeklyRepetitionRecord.getScheduleMonth();
    }

    public int getScheduleDay() {
        return mWeeklyRepetitionRecord.getScheduleDay();
    }
}
