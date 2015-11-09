package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyInstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyInstanceRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public class RealWeeklyInstance extends WeeklyInstance {
    private final WeeklyInstanceRecord mWeeklyInstanceRecord;

    public RealWeeklyInstance(Task task, WeeklyInstanceRecord weeklyInstanceRecord, WeeklyRepetition weeklyRepetition) {
        super(task, weeklyRepetition);
        Assert.assertTrue(weeklyInstanceRecord != null);
        mWeeklyInstanceRecord = weeklyInstanceRecord;
    }

    public int getId() {
        return mWeeklyInstanceRecord.getId();
    }

    public int getTaskId() {
        return mWeeklyInstanceRecord.getTaskId();
    }

    public boolean getDone() {
        return mWeeklyInstanceRecord.getDone();
    }
}
