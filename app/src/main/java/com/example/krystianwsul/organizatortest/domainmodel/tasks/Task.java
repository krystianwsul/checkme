package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

public abstract class Task {
    protected final TaskRecord mTaskRecord;
    protected TreeMap<Integer, ChildTask> mChildrenTasks = new TreeMap<>();

    protected Task(TaskRecord taskRecord) {
        Assert.assertTrue(taskRecord != null);
        mTaskRecord = taskRecord;
    }

    public String getName() {
        return mTaskRecord.getName();
    }

    public Collection<ChildTask> getChildTasks() {
        return mChildrenTasks.values();
    }

    public int getId() {
        return mTaskRecord.getId();
    }

    public int getOrdinal() {
        return mTaskRecord.getOrdinal();
    }

    public int getNextChildOrdinal() {
        if (mChildrenTasks.isEmpty())
            return 0;
        else
            return Collections.max(mChildrenTasks.keySet()) + 1;
    }

    public abstract String getScheduleText(Context context);

    public void addChildTask(ChildTask childTask) {
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(!mChildrenTasks.containsKey(childTask.getOrdinal()));
        mChildrenTasks.put(childTask.getOrdinal(), childTask);
    }

    public abstract RootTask getRootTask();

    public abstract boolean isRootTask();

    public abstract Task getParentTask();
}
