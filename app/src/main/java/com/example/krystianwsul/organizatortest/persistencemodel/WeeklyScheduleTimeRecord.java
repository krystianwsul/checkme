package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class WeeklyScheduleTimeRecord {
    private final int mId;
    private final int mWeeklyScheduleRecordId;

    private final Integer mTimeRecordId;

    private final Integer mHour;
    private final Integer mMinute;

    public WeeklyScheduleTimeRecord(int id, int weeklyScheduleId, Integer timeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (timeId == null));
        Assert.assertTrue((hour != null) || (timeId != null));

        mId = id;
        mWeeklyScheduleRecordId = weeklyScheduleId;

        mTimeRecordId = timeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
    }

    public int getWeeklyScheduleId() {
        return mWeeklyScheduleRecordId;
    }

    public Integer getTimeRecordId() {
        return mTimeRecordId;
    }

    public Integer getHour() {
        return mHour;
    }

    public Integer getMinute() {
        return mMinute;
    }
}
