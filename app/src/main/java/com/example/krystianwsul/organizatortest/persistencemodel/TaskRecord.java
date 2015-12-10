package com.example.krystianwsul.organizatortest.persistencemodel;

import android.text.TextUtils;

import junit.framework.Assert;

public class TaskRecord {
    private final int mId;
    private final Integer mParentTaskId;
    private String mName;
    private final int mOrdinal;

    private final long mStartTime;
    private Long mEndTime;

    TaskRecord(int id, Integer parentId, String name, int ordinal, long startTime, Long endTime) {
        Assert.assertTrue(name != null);

        mId = id;
        mParentTaskId = parentId;
        mName = name;
        mOrdinal = ordinal;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public int getId() {
        return mId;
    }

    public Integer getParentTaskId() {
        return mParentTaskId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        mName = name;
    }

    public int getOrdinal() {
        return mOrdinal;
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
}
