package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.Nullable;

import junit.framework.Assert;

@SuppressWarnings("WeakerAccess")
public abstract class ScheduleJson {
    private long startTime;

    private Long endTime;

    private int type;

    @SuppressWarnings("WeakerAccess")
    public ScheduleJson() {

    }

    ScheduleJson(long startTime, @Nullable Long endTime, int type) {
        Assert.assertTrue((endTime == null) || startTime <= endTime);

        this.startTime = startTime;
        this.endTime = endTime;

        this.type = type;
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
