package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

/**
 * Created by Krystian on 11/3/2015.
 */
public class VirtualDailyInstance extends DailyInstance {
    private final int mId;

    private final boolean mDone = false;

    private static int sVirtualDailyInstanceCount = 0;

    public VirtualDailyInstance(Task task, DailyRepetition dailyRepetition) {
        super(task, dailyRepetition);

        sVirtualDailyInstanceCount++;

        mId = PersistenceManger.getInstance().getMaxDailyInstanceId() + sVirtualDailyInstanceCount;
    }

    public int getId() {
        return mId;
    }
}
