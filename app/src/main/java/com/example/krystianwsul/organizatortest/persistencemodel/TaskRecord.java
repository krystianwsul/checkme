package com.example.krystianwsul.organizatortest.persistencemodel;

import android.text.TextUtils;

import junit.framework.Assert;

public class TaskRecord {
    private final int mId;
    private String mName;

    private final long mStartTime;
    private Long mEndTime;

    TaskRecord(int id, String name, long startTime, Long endTime) {
        Assert.assertTrue(name != null);

        mId = id;
        mName = name;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        mName = name;
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
