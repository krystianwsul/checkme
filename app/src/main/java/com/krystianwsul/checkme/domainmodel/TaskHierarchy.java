package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.TaskHierarchyRecord;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

class TaskHierarchy implements MergedTaskHierarchy {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final TaskHierarchyRecord mTaskHierarchyRecord;

    TaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull TaskHierarchyRecord taskHierarchyRecord) {
        mDomainFactory = domainFactory;
        mTaskHierarchyRecord = taskHierarchyRecord;
    }

    int getId() {
        return mTaskHierarchyRecord.getId();
    }

    @NonNull
    @Override
    public Task getParentTask() {
        MergedTask parentTask = mDomainFactory.getTask(getParentTaskKey());
        Assert.assertTrue(parentTask instanceof Task); // todo firebase

        return (Task) parentTask;
    }

    @NonNull
    @Override
    public Task getChildTask() {
        MergedTask childTask = mDomainFactory.getTask(getChildTaskKey());
        Assert.assertTrue(childTask instanceof Task); // todo firebase

        return (Task) childTask;
    }

    @NonNull
    private ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mTaskHierarchyRecord.getStartTime());
    }

    @Nullable
    private ExactTimeStamp getEndExactTimeStamp() {
        if (mTaskHierarchyRecord.getEndTime() != null)
            return new ExactTimeStamp(mTaskHierarchyRecord.getEndTime());
        else
            return null;
    }

    @Override
    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @Override
    public boolean notDeleted(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0);
    }

    void setEndExactTimeStamp(@NonNull ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(current(endExactTimeStamp));

        mTaskHierarchyRecord.setEndTime(endExactTimeStamp.getLong());
    }

    int getParentTaskId() {
        return mTaskHierarchyRecord.getParentTaskId();
    }

    int getChildTaskId() {
        return mTaskHierarchyRecord.getChildTaskId();
    }

    @NonNull
    @Override
    public TaskKey getParentTaskKey() {
        return new TaskKey(getParentTaskId());
    }

    @NonNull
    @Override
    public TaskKey getChildTaskKey() {
        return new TaskKey(getChildTaskId());
    }
}
