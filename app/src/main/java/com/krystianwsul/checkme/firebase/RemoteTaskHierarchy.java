package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedTask;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

public class RemoteTaskHierarchy {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteTaskHierarchyRecord mRemoteTaskHierarchyRecord;

    public RemoteTaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull String key, @NonNull RemoteTaskHierarchyRecord remoteTaskHierarchyRecord) {
        Assert.assertTrue(!TextUtils.isEmpty(key));

        mDomainFactory = domainFactory;
        mRemoteTaskHierarchyRecord = remoteTaskHierarchyRecord;
    }

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
    public MergedTask getParentTask() {
        return mDomainFactory.getTask(new TaskKey(mRemoteTaskHierarchyRecord.getParentTaskId()));
    }

    @NonNull
    public MergedTask getChildTask() {
        return mDomainFactory.getTask(new TaskKey(mRemoteTaskHierarchyRecord.getChildTaskId()));
    }
}
