package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.persistencemodel.SingleInstanceRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/3/2015.
 */
public class RealSingleInstance extends SingleInstance {
    private final SingleInstanceRecord mSingleInstanceRecord;

    public RealSingleInstance(SingleInstanceRecord singleInstanceRecord) {
        Assert.assertTrue(singleInstanceRecord != null);
        mSingleInstanceRecord = singleInstanceRecord;
    }

    public int getId() {
        return mSingleInstanceRecord.getId();
    }

    public int getTaskId() {
        return mSingleInstanceRecord.getTaskId();
    }

    public int getSingleScheduleId() {
        return mSingleInstanceRecord.getSingleScheduleId();
    }

    public boolean getDone() {
        return mSingleInstanceRecord.getDone();
    }
}
