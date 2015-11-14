package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/31/2015.
 */
public class DailyRepetitionRecord {
    private final int mId;

    private final int mDailyScheduleTimeId;

    private final int mScheduleYear;
    private final int mScheduleMonth;
    private final int mScheduleDay;

    private final Integer mRepetitionYear;
    private final Integer mRepetitionMonth;
    private final Integer mRepetitionDay;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    DailyRepetitionRecord(int id, int dailyScheduleTimeId, int scheduleYear, int scheduleMonth, int scheduleDay, Integer repetitionYear, Integer repetitionMonth, Integer repetitionDay, Integer customTimeId, Integer hour, Integer minute) {
        Assert.assertTrue((repetitionYear == null) == (repetitionMonth == null) == (repetitionDay == null));
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));

        mId = id;

        mDailyScheduleTimeId = dailyScheduleTimeId;

        mScheduleYear = scheduleYear;
        mScheduleMonth = scheduleMonth;
        mScheduleDay = scheduleDay;

        mRepetitionYear = repetitionYear;
        mRepetitionMonth = repetitionMonth;
        mRepetitionDay = repetitionDay;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
    }

    public int getDailyScheduleTimeId() {
        return mDailyScheduleTimeId;
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

    public Integer getCustomTimeId() {
        return mCustomTimeId;
    }

    public Integer getHour() {
        return mHour;
    }

    public Integer getMinute() {
        return mMinute;
    }
}
