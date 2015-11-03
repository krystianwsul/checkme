package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

/**
 * Created by Krystian on 11/3/2015.
 */
public class VirtualWeeklyInstance extends WeeklyInstance {
    private final int mId;

    private final int mTaskId;

    private final int mWeeklyRepetitionId;

    private final boolean mDone = false;

    private static int sVirtualWeeklyInstanceCount = 0;

    public VirtualWeeklyInstance(int taskId, int weeklyRepetitionId) {
        mTaskId = taskId;
        mWeeklyRepetitionId = weeklyRepetitionId;

        sVirtualWeeklyInstanceCount++;

        mId = PersistenceManger.getInstance().getMaxWeeklyInstanceId() + sVirtualWeeklyInstanceCount;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public int getWeeklyRepetitionId() {
        return mWeeklyRepetitionId;
    }

    public boolean getDone() {
        return mDone;
    }
}
