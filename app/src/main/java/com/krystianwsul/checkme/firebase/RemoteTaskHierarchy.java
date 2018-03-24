package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.TaskHierarchy;
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord;
import com.krystianwsul.checkme.utils.TaskHierarchyKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

public class RemoteTaskHierarchy extends TaskHierarchy {
    @NonNull
    private final RemoteProject mRemoteProject;

    @NonNull
    private final RemoteTaskHierarchyRecord mRemoteTaskHierarchyRecord;

    RemoteTaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull RemoteProject remoteProject, @NonNull RemoteTaskHierarchyRecord remoteTaskHierarchyRecord) {
        super(domainFactory);

        mRemoteProject = remoteProject;
        mRemoteTaskHierarchyRecord = remoteTaskHierarchyRecord;
    }

    @NonNull
    @Override
    protected ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mRemoteTaskHierarchyRecord.getStartTime());
    }

    @Nullable
    @Override
    public ExactTimeStamp getEndExactTimeStamp() {
        if (mRemoteTaskHierarchyRecord.getEndTime() != null)
            return new ExactTimeStamp(mRemoteTaskHierarchyRecord.getEndTime());
        else
            return null;
    }

    @NonNull
    @Override
    public TaskKey getParentTaskKey() {
        return new TaskKey(mRemoteProject.getId(), mRemoteTaskHierarchyRecord.getParentTaskId());
    }

    @NonNull
    @Override
    public TaskKey getChildTaskKey() {
        return new TaskKey(mRemoteProject.getId(), mRemoteTaskHierarchyRecord.getChildTaskId());
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

    public void delete() {
        mRemoteProject.deleteTaskHierarchy(this);

        mRemoteTaskHierarchyRecord.delete();
    }

    @NonNull
    @Override
    public RemoteTask getParentTask() {
        return mRemoteProject.getRemoteTaskForce(getParentTaskId());
    }

    @NonNull
    @Override
    public RemoteTask getChildTask() {
        return mRemoteProject.getRemoteTaskForce(getChildTaskId());
    }

    @NonNull
    private String getParentTaskId() {
        return mRemoteTaskHierarchyRecord.getParentTaskId();
    }

    @NonNull
    private String getChildTaskId() {
        return mRemoteTaskHierarchyRecord.getChildTaskId();
    }

    @Override
    public double getOrdinal() {
        return (mRemoteTaskHierarchyRecord.getOrdinal() != null) ? mRemoteTaskHierarchyRecord.getOrdinal() : mRemoteTaskHierarchyRecord.getStartTime();
    }

    @Override
    public void setOrdinal(double ordinal) {
        mRemoteTaskHierarchyRecord.setOrdinal(ordinal);
    }

    @NonNull
    @Override
    public TaskHierarchyKey getTaskHierarchyKey() {
        return new TaskHierarchyKey.RemoteTaskHierarchyKey(mRemoteProject.getId(), mRemoteTaskHierarchyRecord.getId());
    }
}
