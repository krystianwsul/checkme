package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.TaskHierarchyRecord;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

public class LocalTaskHierarchy extends TaskHierarchy {
    @NonNull
    private final TaskHierarchyRecord mTaskHierarchyRecord;

    public LocalTaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull TaskHierarchyRecord taskHierarchyRecord) {
        super(domainFactory);

        mTaskHierarchyRecord = taskHierarchyRecord;
    }

    public int getId() {
        return mTaskHierarchyRecord.getId();
    }

    @NonNull
    @Override
    public ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mTaskHierarchyRecord.getStartTime());
    }

    @Nullable
    @Override
    public ExactTimeStamp getEndExactTimeStamp() {
        if (mTaskHierarchyRecord.getEndTime() != null)
            return new ExactTimeStamp(mTaskHierarchyRecord.getEndTime());
        else
            return null;
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(current(endExactTimeStamp));

        mTaskHierarchyRecord.setEndTime(endExactTimeStamp.getLong());
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

    public int getParentTaskId() {
        return mTaskHierarchyRecord.getParentTaskId();
    }

    public int getChildTaskId() {
        return mTaskHierarchyRecord.getChildTaskId();
    }

    public void delete() {
        mDomainFactory.getLocalFactory().deleteTaskHierarchy(this);

        mTaskHierarchyRecord.delete();
    }
}
