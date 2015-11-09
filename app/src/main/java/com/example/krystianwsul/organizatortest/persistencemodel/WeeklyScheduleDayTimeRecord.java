package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyScheduleDayTimeRecord {
    private final int mId;
    private final int mTaskId;
    private final int mDayOfWeek;

    private final Integer mTimeRecordId;

    private final Integer mHour;
    private final Integer mMinute;

    public WeeklyScheduleDayTimeRecord(int id, int taskId, int dayOfWeek, Integer timeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (timeId == null));
        Assert.assertTrue((hour != null) || (timeId != null));

        mId = id;
        mTaskId = taskId;
        mDayOfWeek = dayOfWeek;

        mTimeRecordId = timeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public int getDayOfWeek() {
        return mDayOfWeek;
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
