package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/3/2015.
 */
public class VirtualSingleInstance extends SingleInstance {
    private final Task mTask;
    private final SingleSchedule mSingleSchedule;

    private final int mId;

    private final boolean mDone = false;

    private static int sVirtualSingleInstanceCount = 0;

    public VirtualSingleInstance(Task task) {
        super(task);

        mTask = task;

        sVirtualSingleInstanceCount++;

        mId = PersistenceManger.getInstance().getMaxSingleInstanceId() + sVirtualSingleInstanceCount;

        mSingleSchedule = SingleSchedule.getSingleSchedule(task.getId());
        Assert.assertTrue(mSingleSchedule != null);
    }

    public int getTaskId() {
        return mTask.getId();
    }

    public boolean getDone() {
        return mDone;
    }

    public String getScheduleText(Context context) {
        return mSingleSchedule.getTaskText(context);
    }
}
