package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

/**
 * Created by Krystian on 11/3/2015.
 */
public class VirtualSingleInstance extends SingleInstance {
    private final int mId;

    private final int mTaskId;

    private final int mSingleScheduleId;

    private final boolean mDone = false;

    private static int sVirtualSingleInstanceCount = 0;

    public VirtualSingleInstance(int taskId, int singleScheduleId) {
        mTaskId = taskId;
        mSingleScheduleId = singleScheduleId;

        sVirtualSingleInstanceCount++;

        mId = PersistenceManger.getInstance().getMaxSingleInstanceId() + sVirtualSingleInstanceCount;
    }


    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public int getSingleScheduleId() {
        return mSingleScheduleId;
    }

    public boolean getDone() {
        return mDone;
    }
}
