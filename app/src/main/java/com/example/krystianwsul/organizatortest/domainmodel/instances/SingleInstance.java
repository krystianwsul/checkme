package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/2/2015.
 */
public class SingleInstance implements Instance {
    private final Task mTask;
    private final SingleRepetition mSingleRepetition;

    private InstanceRecord mInstanceRecord;

    private final int mId;

    public static final String INTENT_KEY = "singleInstanceId";

    SingleInstance(Task task, SingleRepetition singleRepetition, InstanceRecord instanceRecord) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(singleRepetition != null);
        Assert.assertTrue(instanceRecord != null);

        mTask = task;
        mSingleRepetition = singleRepetition;

        mInstanceRecord = instanceRecord;

        mId = mInstanceRecord.getId();
    }

    SingleInstance(Task task, SingleRepetition singleRepetition, int id) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(singleRepetition != null);

        mTask = task;
        mSingleRepetition = singleRepetition;

        mInstanceRecord = null;

        mId = id;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTask.getId();
    }

    public int getRootTaskId() {
        return mSingleRepetition.getRootTaskId();
    }

    public String getName() {
        return mTask.getName();
    }

    public ArrayList<Instance> getChildInstances() {
        ArrayList<Instance> childInstances = new ArrayList<>();
        for (ChildTask childTask : mTask.getChildTasks())
            childInstances.add(InstanceFactory.getInstance().getSingleInstance(childTask, mSingleRepetition));
        return childInstances;
    }

    public String getIntentKey() {
        return INTENT_KEY;
    }

    public int getIntentValue() {
        return mId;
    }

    public String getScheduleText(Context context) {
        if (mTask.getParentTask() == null)
            return getDateTime().getDisplayText(context);
        else
            return null;
    }

    public DateTime getDateTime() {
        return mSingleRepetition.getRepetitionDateTime();
    }

    public TimeStamp getDone() {
        if (mInstanceRecord == null)
            return null;

        Long done = mInstanceRecord.getDone();
        if (done != null)
            return new TimeStamp(done);
        else
            return null;
    }

    public void setDone(boolean done) {
        if (mInstanceRecord == null) {
            if (done)
                mInstanceRecord = PersistenceManger.getInstance().createSingleInstanceRecord(mId, mTask.getId(), mTask.getRootTask().getId(), TimeStamp.getNow().getLong());
            else
                return;
        } else {
            if (done)
                mInstanceRecord.setDone(TimeStamp.getNow().getLong());
            else
                mInstanceRecord.setDone(null);
        }
    }
}