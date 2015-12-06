package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

public class DailyScheduleRecord {
    private final int mRootTaskId;

    private final long mStartTime;
    private final Long mEndTime;

    DailyScheduleRecord(int rootTaskId, long startTime, Long endTime) {
        Assert.assertTrue((endTime == null) || startTime < endTime);

        mRootTaskId = rootTaskId;
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
