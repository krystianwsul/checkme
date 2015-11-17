package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleInstanceRecord;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/2/2015.
 */
public class SingleInstance implements Instance {
    private final Task mTask;
    private final SingleRepetition mSingleRepetition;

    private SingleInstanceRecord mSingleInstanceRecord;

    public static final String INTENT_KEY = "singleInstanceId";

    SingleInstance(Task task, SingleRepetition singleRepetition, SingleInstanceRecord singleInstanceRecord) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(singleRepetition != null);
        Assert.assertTrue(singleInstanceRecord != null);

        mTask = task;
        mSingleRepetition = singleRepetition;

        mSingleInstanceRecord = singleInstanceRecord;
    }

    SingleInstance(Task task, SingleRepetition singleRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(singleRepetition != null);

        mTask = task;
        mSingleRepetition = singleRepetition;

        mSingleInstanceRecord = null;
    }

    public int getTaskId() {
        return mTask.getId();
    }

    public String getName() {
        return mTask.getName();
    }

    public ArrayList<Instance> getChildInstances() {
        ArrayList<Instance> childInstances = new ArrayList<>();
        for (ChildTask childTask : mTask.getChildTasks())
            childInstances.add(SingleInstanceFactory.getInstance().getSingleInstance(childTask, mSingleRepetition));
        return childInstances;
    }

    public String getIntentKey() {
        return INTENT_KEY;
    }

    public int getIntentValue() {
        return mTask.getId();
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
        if (mSingleInstanceRecord == null)
            return null;

        Long done = mSingleInstanceRecord.getDone();
        if (done != null)
            return new TimeStamp(done);
        else
            return null;
    }

    public void setDone(boolean done) {
        if (mSingleInstanceRecord == null) {
            if (done)
                mSingleInstanceRecord = PersistenceManger.getInstance().createSingleInstanceRecord(mTask.getId(), mTask.getRootTask().getId(), TimeStamp.getNow().getLong());
            else
                return;
        } else {
            if (done)
                mSingleInstanceRecord.setDone(TimeStamp.getNow().getLong());
            else
                mSingleInstanceRecord.setDone(null);
        }
    }
}