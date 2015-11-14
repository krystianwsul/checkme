package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyScheduleRecord {
    private final int mId;

    private final int mRootTaskId;

    private final long mStartTime;
    private final Long mEndTime;

    public WeeklyScheduleRecord(int id, int taskId, long startTime, Long endTime) {
        Assert.assertTrue((endTime == null) || startTime < endTime);

        mId = id;
        mRootTaskId = taskId;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public int getId() { return mId; }

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
