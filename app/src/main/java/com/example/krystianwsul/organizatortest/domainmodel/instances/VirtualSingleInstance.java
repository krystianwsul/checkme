package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/3/2015.
 */
public class VirtualSingleInstance extends SingleInstance {
    private final int mId;

    private final boolean mDone = false;

    private static int sVirtualSingleInstanceCount = 0;

    public VirtualSingleInstance(Task task) {
        super(task);

        sVirtualSingleInstanceCount++;

        mId = PersistenceManger.getInstance().getMaxSingleInstanceId() + sVirtualSingleInstanceCount;
    }

    public boolean getDone() {
        return mDone;
    }

    public DateTime getDateTime() {
        return mSingleSchedule.getDateTime();
    }
}
