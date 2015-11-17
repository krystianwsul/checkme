package com.example.krystianwsul.organizatortest.persistencemodel;

/**
 * Created by Krystian on 11/2/2015.
 */
public class SingleInstanceRecord {
    private final int mTaskId;

    private final int mRootTaskId;

    private Long mDone;

    SingleInstanceRecord(int taskId, int rootTaskId, Long done) {
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

    public Long getDone() {
        return mDone;
    }

    public void setDone(Long done) {
        mDone = done;
    }
}
