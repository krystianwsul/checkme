package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskHierarchyRecord;

import junit.framework.Assert;

public class TaskHierarchy {
    private final TaskHierarchyRecord mTaskHierarchyRecord;

    private final Task mParentTask;
    private final Task mChildTask;

    public TaskHierarchy(TaskHierarchyRecord taskHierarchyRecord, Task parentTask, Task childTask) {
        Assert.assertTrue(taskHierarchyRecord != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(childTask != null);

        mTaskHierarchyRecord = taskHierarchyRecord;

        mParentTask = parentTask;
        mChildTask = childTask;
    }

    public int getId() {
        return mTaskHierarchyRecord.getId();
    }

    public Task getParentTask() {
        return mParentTask;
    }

    public Task getChildTask() {
        return mChildTask;
    }

    private TimeStamp getStartTimeStamp() {
        return new TimeStamp(mTaskHierarchyRecord.getStartTime());
    }

    private TimeStamp getEndTimeStamp() {
        if (mTaskHierarchyRecord.getEndTime() != null)
            return new TimeStamp(mTaskHierarchyRecord.getEndTime());
        else
            return null;
    }

    public boolean current(TimeStamp timeStamp) {
        TimeStamp startTimeStamp = getStartTimeStamp();
        TimeStamp endTimeStamp = getEndTimeStamp();

        return (startTimeStamp.compareTo(timeStamp) <= 0 && (endTimeStamp == null || endTimeStamp.compareTo(timeStamp) > 0));
    }
}
