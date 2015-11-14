package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class DailyScheduleRecord {
    private final int mId;

    private final int mRootTaskId;

    private final long mStartTime;
    private final Long mEndTime;

    DailyScheduleRecord(int id, int rootTaskId, long startTime, Long endTime) {
        Assert.assertTrue((endTime == null) || startTime < endTime);

        mId = id;
        mRootTaskId = rootTaskId;
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
