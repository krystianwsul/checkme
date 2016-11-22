package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.Nullable;

import junit.framework.Assert;

@SuppressWarnings("WeakerAccess")
public abstract class ScheduleJson {
    private long startTime;

    private Long endTime;

    @SuppressWarnings("WeakerAccess")
    public ScheduleJson() {

    }

    ScheduleJson(long startTime, @Nullable Long endTime) {
        Assert.assertTrue((endTime == null) || startTime <= endTime);

        this.startTime = startTime;
        this.endTime = endTime;
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
