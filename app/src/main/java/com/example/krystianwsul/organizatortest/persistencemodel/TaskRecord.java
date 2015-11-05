package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class TaskRecord {
    private final int mId;
    private final Integer mParentTaskId;
    private final String mName;

    public TaskRecord(int id, Integer parentId, String name) {
        Assert.assertTrue(name != null);

        mId = id;
        mParentTaskId = parentId;
        mName = name;
    }

    public int getId() {
        return mId;
    }

    public Integer getParentTaskId() {
        return mParentTaskId;
    }

    public String getName() {
        return mName;
    }
}
