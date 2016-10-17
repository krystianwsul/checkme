package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

@SuppressWarnings("WeakerAccess")
public abstract class ScheduleJson {
    private String taskId;

    private long startTime;

    private Long endTime;

    private int type;

    @SuppressWarnings("WeakerAccess")
    public ScheduleJson() {

    }

    ScheduleJson(@NonNull String taskId, long startTime, @Nullable Long endTime, int type) {
        Assert.assertTrue(!TextUtils.isEmpty(taskId));
        Assert.assertTrue((endTime == null) || startTime <= endTime);

        this.taskId = taskId;

        this.startTime = startTime;
        this.endTime = endTime;

        this.type = type;
    }

    @NonNull
    public String getTaskId() {
        return taskId;
    }

    public long getStartTime() {
        return startTime;
    }

    @Nullable
    public Long getEndTime() {
        return endTime;
    }

    public int getType() {
        return type;
    }
}
