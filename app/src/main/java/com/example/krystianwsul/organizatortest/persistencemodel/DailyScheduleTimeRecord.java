package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class DailyScheduleTimeRecord {
    private final int mId;
    private final int taskId;

    private final Integer mTimeRecordId;

    private final Integer mHour;
    private final Integer mMinute;

    public DailyScheduleTimeRecord(int id, int taskId, Integer timeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (timeId == null));
        Assert.assertTrue((hour != null) || (timeId != null));

        mId = id;
        this.taskId = taskId;

        mTimeRecordId = timeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return taskId;
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
