package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedTask;
import com.krystianwsul.checkme.domainmodel.MergedTaskHierarchy;
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

class RemoteTaskHierarchy implements MergedTaskHierarchy {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteTaskHierarchyRecord mRemoteTaskHierarchyRecord;

    RemoteTaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull RemoteTaskHierarchyRecord remoteTaskHierarchyRecord) {
        mDomainFactory = domainFactory;
        mRemoteTaskHierarchyRecord = remoteTaskHierarchyRecord;
    }

    @Override
    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    private ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mRemoteTaskHierarchyRecord.getStartTime());
    }

    @Nullable
    private ExactTimeStamp getEndExactTimeStamp() {
        if (mRemoteTaskHierarchyRecord.getEndTime() != null)
            return new ExactTimeStamp(mRemoteTaskHierarchyRecord.getEndTime());
        else
            return null;
    }

    @NonNull
    @Override
    public MergedTask getParentTask() {
        return mDomainFactory.getTask(getParentTaskKey());
    }

    @NonNull
    @Override
    public MergedTask getChildTask() {
        return mDomainFactory.getTask(getChildTaskKey());
    }

    @NonNull
    @Override
    public TaskKey getParentTaskKey() {
        return new TaskKey(mRemoteTaskHierarchyRecord.getParentTaskId());
    }

    @NonNull
    @Override
    public TaskKey getChildTaskKey() {
        return new TaskKey(mRemoteTaskHierarchyRecord.getChildTaskId());
    }

    @Override
    public boolean notDeleted(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0);
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        mRemoteTaskHierarchyRecord.setEndTime(now.getLong());
    }

    @NonNull
    public String getId() {
        return mRemoteTaskHierarchyRecord.getId();
    }
}
