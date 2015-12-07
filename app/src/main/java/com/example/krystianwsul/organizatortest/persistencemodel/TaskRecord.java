package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

public class TaskRecord {
    private final int mId;
    private final Integer mParentTaskId;
    private final String mName;
    private final int mOrdinal;

    TaskRecord(int id, Integer parentId, String name, int ordinal) {
        Assert.assertTrue(name != null);

        mId = id;
        mParentTaskId = parentId;
        mName = name;
        mOrdinal = ordinal;
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

    public int getOrdinal() {
        return mOrdinal;
    }
}
