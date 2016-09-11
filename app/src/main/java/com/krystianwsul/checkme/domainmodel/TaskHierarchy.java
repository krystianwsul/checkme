package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.persistencemodel.TaskHierarchyRecord;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;

class TaskHierarchy {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final TaskHierarchyRecord mTaskHierarchyRecord;

    TaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull TaskHierarchyRecord taskHierarchyRecord) {
        mDomainFactoryReference = new WeakReference<>(domainFactory);
        mTaskHierarchyRecord = taskHierarchyRecord;
    }

    @NonNull
    private DomainFactory getDomainFactory() {
        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory;
    }

    int getId() {
        return mTaskHierarchyRecord.getId();
    }

    @NonNull
    Task getParentTask() {
        return getDomainFactory().getTask(mTaskHierarchyRecord.getParentTaskId());
    }

    @NonNull
    Task getChildTask() {
        return getDomainFactory().getTask(mTaskHierarchyRecord.getChildTaskId());
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
        Assert.assertTrue(exactTimeStamp != null);

        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    boolean notDeleted(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);

        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0);
    }

    void setEndExactTimeStamp(ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(endExactTimeStamp != null);
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
