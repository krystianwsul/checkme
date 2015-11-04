package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/31/2015.
 */
public class WeeklyRepetitionRecord {
    private final int mId;

    private final int mWeeklyScheduleTimeId;

    private final int mScheduleYear;
    private final int mScheduleMonth;
    private final int mScheduleDay;

    private final Integer mRepetitionYear;
    private final Integer mRepetitionMonth;
    private final Integer mRepetitionDay;

    private final Integer mTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    public WeeklyRepetitionRecord(int id, int weeklyScheduleTimeId, int scheduleYear, int scheduleMonth, int scheduleDay, Integer repetitionYear, Integer repetitionMonth, Integer repetitionDay, Integer timeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (timeId == null));

        mId = id;

        mWeeklyScheduleTimeId = weeklyScheduleTimeId;

        mScheduleYear = scheduleYear;
        mScheduleMonth = scheduleMonth;
        mScheduleDay = scheduleDay;

        mRepetitionYear = repetitionYear;
        mRepetitionMonth = repetitionMonth;
        mRepetitionDay = repetitionDay;

        mTimeId = timeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
    }

    public int getWeeklyScheduleTimeId() {
        return mWeeklyScheduleTimeId;
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
        return mRepetitionYear;
    }

    public Integer getRepetitionMonth() {
        return mRepetitionMonth;
    }

    public Integer getRepetitionDay() {
        return mRepetitionDay;
    }

    public Integer getTimeId() {
        return mTimeId;
    }

    public Integer getHour() {
        return mHour;
    }

    public Integer getMinute() {
        return mMinute;
    }
}
