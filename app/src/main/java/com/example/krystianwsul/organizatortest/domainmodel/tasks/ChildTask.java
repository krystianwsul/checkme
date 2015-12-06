package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

public class ChildTask extends Task {
    private final Task mParentTask;

    ChildTask(TaskRecord taskRecord, Task parentTask) {
        super(taskRecord);

        Assert.assertTrue(mTaskRecord.getParentTaskId() != null);

        Assert.assertTrue(parentTask != null);
        mParentTask = parentTask;
    }

    public String getScheduleText(Context context) {
        return null;
    }

    public RootTask getRootTask() {
        return mParentTask.getRootTask();
    }

    public boolean isRootTask() {
        return false;
    }
}
