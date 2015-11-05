package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/2/2015.
 */
public class SingleInstanceRecord {
    private final int mTaskId;

    private final boolean mDone;

    public SingleInstanceRecord(int taskId, boolean done) {
        mTaskId = taskId;

        mDone = done;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public boolean getDone() {
        return mDone;
    }
}
