package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/18/2015.
 */
public class InstanceRecord {
    private final int mId;
    private final int mTaskId;

    private Long mDone;

    private final Integer mRootTaskId;
    private final Integer mDailyRepetitionId;
    private final Integer mWeeklyRepetitionId;

    public InstanceRecord(int id, int taskId, Long done, Integer rootTaskId, Integer dailyRepetitionId, Integer weeklyRepetitionId) {
        Assert.assertTrue((rootTaskId != null ? 1 : 0) + (dailyRepetitionId != null ? 1 : 0) + (weeklyRepetitionId != null ? 1 : 0) == 1);

        mId = id;
        mTaskId = taskId;

        mDone = done;

        mRootTaskId = rootTaskId;
        mDailyRepetitionId = dailyRepetitionId;
        mWeeklyRepetitionId = weeklyRepetitionId;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public Long getDone() {
        return mDone;
    }

    public Integer getRootTaskId() {
        return mRootTaskId;
    }

    public Integer getDailyRepetitionId() {
        return mDailyRepetitionId;
    }

    public Integer getWeeklyRepetitionId() {
        return mWeeklyRepetitionId;
    }

    public void setDone(Long done) {
        mDone = done;
    }
}
