package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

public class RemoteTaskHierarchyRecord {
    private String mParentTaskId;
    private String mChildTaskId;

    private long mStartTime;
    private Long mEndTime;

    public RemoteTaskHierarchyRecord() {

    }

    RemoteTaskHierarchyRecord(@NonNull String parentTaskId, @NonNull String childTaskId, long startTime, Long endTime) {
        Assert.assertTrue(!TextUtils.isEmpty(parentTaskId));
        Assert.assertTrue(!TextUtils.isEmpty(childTaskId));
        Assert.assertTrue(!parentTaskId.equals(childTaskId));
        Assert.assertTrue(endTime == null || startTime <= endTime);

        mParentTaskId = parentTaskId;
        mChildTaskId = childTaskId;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    @NonNull
    public String getParentTaskId() {
        return mParentTaskId;
    }

    @NonNull
    public String getChildTaskId() {
        return mChildTaskId;
    }

    public long getStartTime() {
        return mStartTime;
    }

    @Nullable
    public Long getEndTime() {
        return mEndTime;
    }
}
