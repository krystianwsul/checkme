package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.TaskHierarchyRecord;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

class TaskHierarchy {
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
    Task getParentTask() {
        return mDomainFactory.getTask(mTaskHierarchyRecord.getParentTaskId());
    }

    @NonNull
    Task getChildTask() {
        return mDomainFactory.getTask(mTaskHierarchyRecord.getChildTaskId());
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

    boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    boolean notDeleted(@NonNull ExactTimeStamp exactTimeStamp) {
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
}
