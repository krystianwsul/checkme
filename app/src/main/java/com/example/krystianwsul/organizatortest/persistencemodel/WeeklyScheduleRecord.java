package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

public class WeeklyScheduleRecord {
    private final int mRootTaskId;

    private final long mStartTime;
    private final Long mEndTime;

    WeeklyScheduleRecord(int taskId, long startTime, Long endTime) {
        Assert.assertTrue((endTime == null) || startTime < endTime);

        mRootTaskId = taskId;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public int getRootTaskId() {
        return mRootTaskId;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public Long getEndTime() {
        return mEndTime;
    }
}
