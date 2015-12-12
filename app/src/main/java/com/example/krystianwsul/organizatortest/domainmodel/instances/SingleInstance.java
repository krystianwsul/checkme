package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

public class SingleInstance extends Instance {
    private final SingleRepetition mSingleRepetition;

    SingleInstance(Task task, SingleRepetition singleRepetition, InstanceRecord instanceRecord) {
        super(task, instanceRecord);

        Assert.assertTrue(singleRepetition != null);
        mSingleRepetition = singleRepetition;
    }

    SingleInstance(Task task, SingleRepetition singleRepetition, int id) {
        super(task, id);

        Assert.assertTrue(singleRepetition != null);
        mSingleRepetition = singleRepetition;
    }

    public int getRootTaskId() {
        return mSingleRepetition.getRootTaskId();
    }

    public Instance getChildInstance(Task childTask) {
        Assert.assertTrue(childTask != null);
        return InstanceFactory.getInstance().getSingleInstance(childTask, mSingleRepetition);
    }

    public String getScheduleText(Context context) {
        Assert.assertTrue(context != null);

        if (isRootInstance())
            return getDateTime().getDisplayText(context);
        else
            return null;
    }

    public DateTime getDateTime() {
        return mSingleRepetition.getRepetitionDateTime();
    }

    protected InstanceRecord createInstanceRecord(TimeStamp timeStamp) {
        return PersistenceManger.getInstance().createSingleInstanceRecord(mId, mTask, mTask.getRootTask(timeStamp), timeStamp.getLong());
    }

    public boolean isRootInstance() {
        return mTask.isRootTask(mSingleRepetition.getRepetitionDateTime().getTimeStamp());
    }
}