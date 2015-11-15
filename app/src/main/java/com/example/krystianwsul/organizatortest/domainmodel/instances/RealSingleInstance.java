package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleInstanceRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/3/2015.
 */
public class RealSingleInstance extends SingleInstance {
    private final SingleInstanceRecord mSingleInstanceRecord;

    public RealSingleInstance(Task task, SingleInstanceRecord singleInstanceRecord, SingleRepetition singleRepetition) {
        super(task, singleRepetition);

        Assert.assertTrue(singleInstanceRecord != null);
        mSingleInstanceRecord = singleInstanceRecord;
    }
}
