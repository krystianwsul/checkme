package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

/**
 * Created by Krystian on 11/3/2015.
 */
public class VirtualWeeklyInstance extends WeeklyInstance {
    private final int mId;

    private final boolean mDone = false;

    private static int sVirtualWeeklyInstanceCount = 0;

    public VirtualWeeklyInstance(Task task, WeeklyRepetition weeklyRepetition) {
        super(task, weeklyRepetition);

        sVirtualWeeklyInstanceCount++;

        mId = PersistenceManger.getInstance().getMaxWeeklyInstanceId() + sVirtualWeeklyInstanceCount;
    }

    public int getId() {
        return mId;
    }

    public boolean getDone() {
        return mDone;
    }
}
