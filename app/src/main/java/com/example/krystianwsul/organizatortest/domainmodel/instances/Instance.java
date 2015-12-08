package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.InstanceRecord;

import junit.framework.Assert;

import java.util.ArrayList;

public abstract class Instance {
    protected final Task mTask;

    protected InstanceRecord mInstanceRecord;

    protected final int mId;

    protected Instance(Task task, InstanceRecord instanceRecord) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(instanceRecord != null);

        mTask = task;

        mInstanceRecord = instanceRecord;

        mId = mInstanceRecord.getId();
    }

    protected Instance(Task task, int id) {
        Assert.assertTrue(task != null);

        mTask = task;

        mInstanceRecord = null;

        mId = id;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTask.getId();
    }

    public String getName() {
        return mTask.getName();
    }

    public abstract String getScheduleText(Context context);

    public ArrayList<Instance> getChildInstances() {
        ArrayList<Instance> childInstances = new ArrayList<>();
        for (ChildTask childTask : mTask.getChildTasks(getDateTime().getTimeStamp())) {
            Instance childInstance = getChildInstance(childTask);
            Assert.assertTrue(childInstance != null);
            childInstances.add(childInstance);
        }
        return childInstances;
    }

    public boolean isRootInstance() {
        return (mTask.isRootTask());
    }

    public abstract DateTime getDateTime();

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
                mInstanceRecord = createInstanceRecord();
        } else {
            if (done)
                mInstanceRecord.setDone(TimeStamp.getNow().getLong());
            else
                mInstanceRecord.setDone(null);
        }
    }

    protected abstract InstanceRecord createInstanceRecord();

    protected abstract Instance getChildInstance(ChildTask childTask);
}
