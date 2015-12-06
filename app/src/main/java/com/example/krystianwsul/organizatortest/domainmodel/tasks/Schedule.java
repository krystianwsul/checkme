package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import junit.framework.Assert;

import java.util.ArrayList;

public abstract class Schedule {
    protected final RootTask mRootTask;

    public abstract String getTaskText(Context context);
    public abstract ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp);

    protected Schedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        mRootTask = rootTask;
    }

    public int getRootTaskId() {
        return mRootTask.getId();
    }

    public abstract TimeStamp getEndTimeStamp();

    public abstract boolean isMutable();

    abstract Schedule copy(RootTask newRootTask);

    public abstract boolean current();
}
