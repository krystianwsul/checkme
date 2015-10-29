package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class WeeklyScheduleRecord {
    private final int mId;

    private final long mStartTime;
    private final Long mEndTime;

    public WeeklyScheduleRecord(int id, long startTime, Long endTime) {
        Assert.assertTrue((endTime == null) || startTime < endTime);

        mId = id;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public int getId() {
        return mId;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public Long getEndTime() {
        return mEndTime;
    }
}
