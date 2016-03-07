package com.example.krystianwsul.organizator.domainmodel;

import com.example.krystianwsul.organizator.persistencemodel.TaskHierarchyRecord;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;

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

    private ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mTaskHierarchyRecord.getStartTime());
    }

    private ExactTimeStamp getEndExactTimeStamp() {
        if (mTaskHierarchyRecord.getEndTime() != null)
            return new ExactTimeStamp(mTaskHierarchyRecord.getEndTime());
        else
            return null;
    }

    boolean current(ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    void setEndExactTimeStamp(ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(endExactTimeStamp != null);
        Assert.assertTrue(current(endExactTimeStamp));

        mTaskHierarchyRecord.setEndTime(endExactTimeStamp.getLong());
    }
}
