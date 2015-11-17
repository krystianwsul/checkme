package com.example.krystianwsul.organizatortest.persistencemodel;

/**
 * Created by Krystian on 11/2/2015.
 */
public class DailyInstanceRecord {
    private final int mId;

    private final int mTaskId;

    private final int mDailyRepetitionId;

    private Long mDone;

    DailyInstanceRecord(int id, int taskId, int dailyRepetitionId, Long done) {
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

    public Long getDone() {
        return mDone;
    }

    public void setDone(Long done) {
        mDone = done;
    }
}
