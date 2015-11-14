package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

/**
 * Created by Krystian on 11/3/2015.
 */
public class VirtualSingleInstance extends SingleInstance {
    private final int mId;

    private final boolean mDone = false;

    private static int sVirtualSingleInstanceCount = 0;

    public VirtualSingleInstance(Task task, SingleRepetition singleRepetition) {
        super(task, singleRepetition);

        sVirtualSingleInstanceCount++;

        mId = PersistenceManger.getInstance().getMaxSingleInstanceId() + sVirtualSingleInstanceCount;
    }

    public boolean getDone() {
        return mDone;
    }
}
