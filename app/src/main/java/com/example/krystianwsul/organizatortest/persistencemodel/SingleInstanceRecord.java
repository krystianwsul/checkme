package com.example.krystianwsul.organizatortest.persistencemodel;

/**
 * Created by Krystian on 11/2/2015.
 */
public class SingleInstanceRecord {
    private final int mTaskId;

    private final int mRootTaskId;

    SingleInstanceRecord(int taskId, int rootTaskId) {
        mTaskId = taskId;

        mRootTaskId = rootTaskId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public int getRootTaskId() {
        return mRootTaskId;
    }
}
