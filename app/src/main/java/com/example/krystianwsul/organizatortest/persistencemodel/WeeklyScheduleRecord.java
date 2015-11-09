package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyScheduleRecord {
    private final int mTaskId;

    private final long mStartTime;
    private final Long mEndTime;

    public WeeklyScheduleRecord(int taskId, long startTime, Long endTime) {
        Assert.assertTrue((endTime == null) || startTime < endTime);

        mTaskId = taskId;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public Long getEndTime() {
        return mEndTime;
    }
}
