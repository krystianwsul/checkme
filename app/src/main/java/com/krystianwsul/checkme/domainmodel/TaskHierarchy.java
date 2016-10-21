package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

public abstract class TaskHierarchy {
    @NonNull
    protected final DomainFactory mDomainFactory;

    protected TaskHierarchy(@NonNull DomainFactory domainFactory) {
        mDomainFactory = domainFactory;
    }

    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    protected abstract ExactTimeStamp getStartExactTimeStamp();

    @Nullable
    protected abstract ExactTimeStamp getEndExactTimeStamp();

    @NonNull
    public abstract TaskKey getParentTaskKey();

    @NonNull
    public abstract TaskKey getChildTaskKey();

    @NonNull
    public MergedTask getParentTask() {
        return mDomainFactory.getTask(getParentTaskKey());
    }

    @NonNull
    public MergedTask getChildTask() {
        return mDomainFactory.getTask(getChildTaskKey());
    }

    public boolean notDeleted(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0);
    }

    public abstract void setEndExactTimeStamp(@NonNull ExactTimeStamp now);
}
