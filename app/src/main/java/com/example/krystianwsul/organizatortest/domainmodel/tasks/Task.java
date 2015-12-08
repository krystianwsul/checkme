package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.util.ArrayList;
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

    public Collection<ChildTask> getChildTasks(TimeStamp timeStamp) {
        ArrayList<ChildTask> ret = new ArrayList<>();
        for (ChildTask childTask : mChildrenTasks.values())
            if (childTask.current(timeStamp))
                ret.add(childTask);
        return ret;
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

    private TimeStamp getStartTimeStamp() {
        return new TimeStamp(mTaskRecord.getStartTime());
    }

    private TimeStamp getEndTimeStamp() {
        if (mTaskRecord.getEndTime() != null)
            return new TimeStamp(mTaskRecord.getEndTime());
        else
            return null;
    }

    public void setEndTimeStamp() {
        mTaskRecord.setEndTime(TimeStamp.getNow().getLong());
    }

    public boolean current(TimeStamp timeStamp) {
        TimeStamp startTimeStamp = getStartTimeStamp();
        TimeStamp endTimeStamp = getEndTimeStamp();

        return (startTimeStamp.compareTo(timeStamp) <= 0 && (endTimeStamp == null || endTimeStamp.compareTo(timeStamp) > 0));
    }
}
