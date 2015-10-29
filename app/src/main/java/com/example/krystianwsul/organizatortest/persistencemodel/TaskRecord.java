package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class TaskRecord {
    private final int mId;
    private final Integer mParentTaskId;
    private final String mName;
    private final Integer mSingleScheduleId;
    private final Integer mWeeklyScheduleId;

    public TaskRecord(int id, Integer parentId, String name, Integer singleScheduleId, Integer weeklyScheduleId) {
        Assert.assertTrue(name != null);
        Assert.assertTrue(parentId == null || (singleScheduleId == null && weeklyScheduleId == null));
        Assert.assertTrue(parentId != null || singleScheduleId != null || weeklyScheduleId != null);
        Assert.assertTrue(singleScheduleId == null || weeklyScheduleId == null);

        mId = id;
        mParentTaskId = parentId;
        mName = name;
        mSingleScheduleId = singleScheduleId;
        mWeeklyScheduleId = weeklyScheduleId;
    }

    public int getId() {
        return mId;
    }

    public Integer getParentTaskId() {
        return mParentTaskId;
    }

    public Integer getSingleScheduleId() {
        return mSingleScheduleId;
    }

    public Integer getWeeklyScheduleId() {
        return mWeeklyScheduleId;
    }

    public String getName() {
        return mName;
    }
}
