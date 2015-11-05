package com.example.krystianwsul.organizatortest.persistencemodel;

/**
 * Created by Krystian on 11/2/2015.
 */
public class DailyInstanceRecord {
    private final int mId;

    private final int mTaskId;

    private final int mDailyRepetitionId;

    private final boolean mDone;

    public DailyInstanceRecord(int id, int taskId, int dailyRepetitionId, boolean done) {
        mId = id;

        mTaskId = taskId;

        mDailyRepetitionId = dailyRepetitionId;

        mDone = done;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public int getDailyRepetitionId() {
        return mDailyRepetitionId;
    }

    public boolean getDone() {
        return mDone;
    }
}
