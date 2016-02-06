package com.example.krystianwsul.organizator.domainmodel;

import com.example.krystianwsul.organizator.persistencemodel.TaskHierarchyRecord;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;

class TaskHierarchy {
    private final TaskHierarchyRecord mTaskHierarchyRecord;

    private final WeakReference<Task> mParentTaskReference;
    private final Task mChildTask;

    TaskHierarchy(TaskHierarchyRecord taskHierarchyRecord, Task parentTask, Task childTask) {
        Assert.assertTrue(taskHierarchyRecord != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(childTask != null);

        mTaskHierarchyRecord = taskHierarchyRecord;

        mParentTaskReference = new WeakReference<>(parentTask);
        mChildTask = childTask;
    }

    int getId() {
        return mTaskHierarchyRecord.getId();
    }

    Task getParentTask() {
        Task parentTask = mParentTaskReference.get();
        Assert.assertTrue(parentTask != null);

        return parentTask;
    }

    Task getChildTask() {
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

    boolean current(TimeStamp timeStamp) {
        TimeStamp startTimeStamp = getStartTimeStamp();
        TimeStamp endTimeStamp = getEndTimeStamp();

        return (startTimeStamp.compareTo(timeStamp) <= 0 && (endTimeStamp == null || endTimeStamp.compareTo(timeStamp) > 0));
    }

    void setEndTimeStamp(TimeStamp endTimeStamp) {
        Assert.assertTrue(endTimeStamp != null);
        Assert.assertTrue(current(endTimeStamp));

        mTaskHierarchyRecord.setEndTime(endTimeStamp.getLong());
    }
}
