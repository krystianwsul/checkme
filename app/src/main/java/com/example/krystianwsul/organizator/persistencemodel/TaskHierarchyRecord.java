package com.example.krystianwsul.organizator.persistencemodel;

import junit.framework.Assert;

public class TaskHierarchyRecord {
    private final int mId;

    private final int mParentTaskId;
    private final int mChildTaskId;

    private final long mStartTime;
    private Long mEndTime;

    public TaskHierarchyRecord(int id, int parentTaskId, int childTaskId, long startTime, Long endTime) {
        Assert.assertTrue(parentTaskId != childTaskId);
        Assert.assertTrue(endTime == null || startTime <= endTime);

        mId = id;
        mParentTaskId = parentTaskId;
        mChildTaskId = childTaskId;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public int getId() {
        return mId;
    }

    public int getParentTaskId() {
        return mParentTaskId;
    }

    public int getChildTaskId() {
        return mChildTaskId;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public Long getEndTime() {
        return mEndTime;
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(mEndTime == null);
        Assert.assertTrue(mStartTime <= endTime);

        mEndTime = endTime;
    }
}
