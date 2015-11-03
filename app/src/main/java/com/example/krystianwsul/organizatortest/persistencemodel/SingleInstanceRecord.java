package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/2/2015.
 */
public class SingleInstanceRecord {
    private final int mId;

    private final int mTaskId;

    private final int mSingleScheduleId;

    private final boolean mDone;

    public SingleInstanceRecord(int id, int taskId, int singleScheduleId, boolean done) {
        mId = id;

        mTaskId = taskId;

        mSingleScheduleId = singleScheduleId;

        mDone = done;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public int getSingleScheduleId() {
        return mSingleScheduleId;
    }

    public boolean getDone() {
        return mDone;
    }
}
