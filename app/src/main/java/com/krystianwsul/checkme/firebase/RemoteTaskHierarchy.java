package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedTask;
import com.krystianwsul.checkme.domainmodel.MergedTaskHierarchy;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.Map;

public class RemoteTaskHierarchy implements MergedTaskHierarchy {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final String mId;

    @NonNull
    private final RemoteTaskHierarchyRecord mRemoteTaskHierarchyRecord;

    public RemoteTaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull String id, @NonNull RemoteTaskHierarchyRecord remoteTaskHierarchyRecord) {
        Assert.assertTrue(!TextUtils.isEmpty(id));

        mId = id;
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

    void setEndExactTimeStamp(@NonNull Map<String, Object> values, @NonNull ExactTimeStamp now) {
        values.put("tasks/" + mId + "/taskHierarchyRecord/endTime", now.getLong());
    }
}
