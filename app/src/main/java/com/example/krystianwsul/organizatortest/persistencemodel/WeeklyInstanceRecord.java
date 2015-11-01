package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/31/2015.
 */
public class WeeklyInstanceRecord {
    private final int mId;

    private final int mWeeklyScheduleTimeId;

    private final int mScheduleYear;
    private final int mScheduleMonth;
    private final int mScheduleDay;

    private final Integer mInstanceYear;
    private final Integer mInstanceMonth;
    private final Integer mInstanceDay;

    private final Integer mTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    private final boolean mDone;

    public WeeklyInstanceRecord(int id, int weeklyScheduleTimeId, int scheduleYear, int scheduleMonth, int scheduleDay, Integer instanceYear, Integer instanceMonth, Integer instanceDay, Integer timeId, Integer hour, Integer minute, boolean done) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (timeId == null));

        mId = id;

        mWeeklyScheduleTimeId = weeklyScheduleTimeId;

        mScheduleYear = scheduleYear;
        mScheduleMonth = scheduleMonth;
        mScheduleDay = scheduleDay;

        mInstanceYear = instanceYear;
        mInstanceMonth = instanceMonth;
        mInstanceDay = instanceDay;

        mTimeId = timeId;

        mHour = hour;
        mMinute = minute;

        mDone = done;
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

    public Integer getInstanceYear() {
        return mInstanceYear;
    }

    public Integer getInstanceMonth() {
        return mInstanceMonth;
    }

    public Integer getInstanceDay() {
        return mInstanceDay;
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

    public boolean getDone() {
        return mDone;
    }
}
