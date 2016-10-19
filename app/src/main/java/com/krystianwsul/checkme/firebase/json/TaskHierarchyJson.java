package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

public class TaskHierarchyJson {
    private String parentTaskId;
    private String childTaskId;

    private long startTime;
    private Long endTime;

    public TaskHierarchyJson() {

    }

    public TaskHierarchyJson(@NonNull String parentTaskId, @NonNull String childTaskId, long startTime, Long endTime) {
        Assert.assertTrue(!TextUtils.isEmpty(parentTaskId));
        Assert.assertTrue(!TextUtils.isEmpty(childTaskId));
        Assert.assertTrue(!parentTaskId.equals(childTaskId));
        Assert.assertTrue(endTime == null || startTime <= endTime);

        this.parentTaskId = parentTaskId;
        this.childTaskId = childTaskId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @NonNull
    public String getParentTaskId() {
        Assert.assertTrue(!TextUtils.isEmpty(parentTaskId));
        return parentTaskId;
    }

    @NonNull
    public String getChildTaskId() {
        Assert.assertTrue(!TextUtils.isEmpty(parentTaskId));
        return childTaskId;
    }

    public long getStartTime() {
        return startTime;
    }

    @Nullable
    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
