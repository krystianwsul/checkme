package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

public class DailyInstance extends Instance {
    private final DailyRepetition mDailyRepetition;

    DailyInstance(Task task, DailyRepetition dailyRepetition, InstanceRecord instanceRecord) {
        super(task, instanceRecord);

        Assert.assertTrue(dailyRepetition != null);
        mDailyRepetition = dailyRepetition;
    }

    DailyInstance(Task task, DailyRepetition dailyRepetition, int id) {
        super(task, id);

        Assert.assertTrue(dailyRepetition != null);
        mDailyRepetition = dailyRepetition;
    }

    public int getDailyRepetitionId() {
        return mDailyRepetition.getId();
    }

    public String getScheduleText(Context context) {
        Assert.assertTrue(context != null);

        if (isRootInstance())
            return getDateTime().getDisplayText(context);
        else
            return null;
    }

    public Instance getChildInstance(ChildTask childTask) {
        Assert.assertTrue(childTask != null);
        return InstanceFactory.getInstance().getDailyInstance(childTask, mDailyRepetition);
    }

    public DateTime getDateTime() {
        return mDailyRepetition.getRepetitionDateTime();
    }

    protected InstanceRecord createInstanceRecord() {
        return PersistenceManger.getInstance().createDailyInstanceRecord(mId, mTask, mDailyRepetition, TimeStamp.getNow().getLong());
    }
}
