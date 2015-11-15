package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyInstanceRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/3/2015.
 */
public class RealDailyInstance extends DailyInstance {
    private final DailyInstanceRecord mDailyInstanceRecord;

    public RealDailyInstance(Task task, DailyInstanceRecord dailyInstanceRecord, DailyRepetition dailyRepetition) {
        super(task, dailyRepetition);
        Assert.assertTrue(dailyInstanceRecord != null);
        mDailyInstanceRecord = dailyInstanceRecord;
    }

    public int getId() {
        return mDailyInstanceRecord.getId();
    }

    public int getTaskId() {
        return mDailyInstanceRecord.getTaskId();
    }
}
