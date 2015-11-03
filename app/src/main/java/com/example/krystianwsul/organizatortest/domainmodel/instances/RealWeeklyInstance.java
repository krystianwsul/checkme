package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyInstanceRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/3/2015.
 */
public class RealWeeklyInstance extends WeeklyInstance {
    private final WeeklyInstanceRecord mWeeklyInstanceRecord;

    public RealWeeklyInstance(WeeklyInstanceRecord weeklyInstanceRecord) {
        Assert.assertTrue(weeklyInstanceRecord != null);
        mWeeklyInstanceRecord = weeklyInstanceRecord;
    }

    public int getId() {
        return mWeeklyInstanceRecord.getId();
    }

    public int getTaskId() {
        return mWeeklyInstanceRecord.getTaskId();
    }

    public int getWeeklyRepetitionId() {
        return mWeeklyInstanceRecord.getWeeklyRepetitionId();
    }

    public boolean getDone() {
        return mWeeklyInstanceRecord.getDone();
    }
}
