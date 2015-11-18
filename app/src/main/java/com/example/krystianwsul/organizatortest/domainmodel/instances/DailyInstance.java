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

import java.util.ArrayList;

/**
 * Created by Krystian on 11/2/2015.
 */
public class DailyInstance implements Instance {
    private final Task mTask;
    private final DailyRepetition mDailyRepetition;

    private InstanceRecord mInstanceRecord;

    private final int mId;

    public static final String INTENT_KEY = "dailyInstanceId";

    DailyInstance(Task task, DailyRepetition dailyRepetition, InstanceRecord instanceRecord) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);
        Assert.assertTrue(instanceRecord != null);

        mTask = task;
        mDailyRepetition = dailyRepetition;

        mInstanceRecord = instanceRecord;

        mId = mInstanceRecord.getId();
    }

    DailyInstance(Task task, DailyRepetition dailyRepetition, int id) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);

        mTask = task;
        mDailyRepetition = dailyRepetition;

        mInstanceRecord = null;

        mId = id;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTask.getId();
    }

    public int getDailyRepetitionId() {
        return mDailyRepetition.getId();
    }

    public String getName() {
        return mTask.getName();
    }

    public String getScheduleText(Context context) {
        if (mTask.getParentTask() == null)
            return getDateTime().getDisplayText(context);
        else
            return null;
    }

    public ArrayList<Instance> getChildInstances() {
        ArrayList<Instance> childInstances = new ArrayList<>();
        for (ChildTask childTask : mTask.getChildTasks())
            childInstances.add(InstanceFactory.getInstance().getDailyInstance(childTask, mDailyRepetition));
        return childInstances;
    }

    public String getIntentKey() {
        return INTENT_KEY;
    }

    public int getIntentValue() {
        return getId();
    }

    public DateTime getDateTime() {
        return mDailyRepetition.getRepetitionDateTime();
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
                mInstanceRecord = PersistenceManger.getInstance().createDailyInstanceRecord(mId, mTask.getId(), mDailyRepetition.getId(), TimeStamp.getNow().getLong());
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
