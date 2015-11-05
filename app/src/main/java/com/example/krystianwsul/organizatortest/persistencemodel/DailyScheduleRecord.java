package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class DailyScheduleRecord {
    private final int mTaskId;

    private final long mStartTime;
    private final Long mEndTime;

    public DailyScheduleRecord(int taskId, long startTime, Long endTime) {
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
