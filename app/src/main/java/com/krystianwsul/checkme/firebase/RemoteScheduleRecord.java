package com.krystianwsul.checkme.firebase;

import android.support.annotation.Nullable;

import junit.framework.Assert;

public abstract class RemoteScheduleRecord {
    private final long startTime;

    @Nullable
    private Long endTime;

    private final int type;

    RemoteScheduleRecord(long startTime, @Nullable Long endTime, int type) {
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
