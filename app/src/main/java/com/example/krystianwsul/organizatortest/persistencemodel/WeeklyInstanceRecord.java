package com.example.krystianwsul.organizatortest.persistencemodel;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyInstanceRecord {
    private final int mId;

    private final int mTaskId;

    private final int mWeeklyRepetitionId;

    WeeklyInstanceRecord(int id, int taskId, int weeklyRepetitionId) {
        mId = id;

        mTaskId = taskId;

        mWeeklyRepetitionId = weeklyRepetitionId;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public int getWeeklyRepetitionId() {
        return mWeeklyRepetitionId;
    }
}
