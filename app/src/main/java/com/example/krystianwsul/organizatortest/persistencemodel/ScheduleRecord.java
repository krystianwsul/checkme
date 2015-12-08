package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

public class ScheduleRecord {
    private final int mId;
    private final int mRootTaskId;

    private final long mStartTime;
    private Long mEndTime;

    private int mType;

    public ScheduleRecord(int id, int rootTaskId, long startTime, Long endTime, int type) {
        Assert.assertTrue((endTime == null) || startTime < endTime);

        mId = id;
        mRootTaskId = rootTaskId;

        mStartTime = startTime;
        mEndTime = endTime;

        mType = type;
    }

    public int getId() {
        return mId;
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

    public void setEndTime(long endTime) {
        mEndTime = endTime;
    }

    public int getType() {
        return mType;
    }
}
