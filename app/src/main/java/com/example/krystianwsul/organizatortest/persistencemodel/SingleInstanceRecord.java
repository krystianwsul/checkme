package com.example.krystianwsul.organizatortest.persistencemodel;

/**
 * Created by Krystian on 11/2/2015.
 */
public class SingleInstanceRecord {
    private final int mTaskId;

    private final int mRootTaskId;

    private final boolean mDone;

    SingleInstanceRecord(int taskId, int rootTaskId, boolean done) {
        mTaskId = taskId;

        mRootTaskId = rootTaskId;

        mDone = done;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public int getRootTaskId() {
        return mRootTaskId;
    }

    public boolean getDone() {
        return mDone;
    }
}
