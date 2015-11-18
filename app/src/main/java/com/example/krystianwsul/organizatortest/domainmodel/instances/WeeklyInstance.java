package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyInstance implements Instance {
    private final Task mTask;
    private final WeeklyRepetition mWeeklyRepetition;

    private InstanceRecord mInstanceRecord;

    private final int mId;

    public static final String INTENT_KEY = "weeklyInstanceId";

    WeeklyInstance(Task task, WeeklyRepetition weeklyRepetition, InstanceRecord instanceRecord) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);
        Assert.assertTrue(instanceRecord != null);

        mTask = task;
        mWeeklyRepetition = weeklyRepetition;

        mInstanceRecord = instanceRecord;

        mId = mInstanceRecord.getId();
    }

    WeeklyInstance(Task task, WeeklyRepetition weeklyRepetition, int id) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);

        mTask = task;
        mWeeklyRepetition = weeklyRepetition;

        mInstanceRecord = null;

        mId = id;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTask.getId();
    }

    public int getWeeklyRepetitionId() {
        return mWeeklyRepetition.getId();
    }

    public String getName() {
        return mTask.getName();
    }

    public String getScheduleText(Context context) {
        if (mTask.getParentTask() == null)
            return mWeeklyRepetition.getRepetitionDateTime().getDisplayText(context);
        else
            return null;
    }

    public ArrayList<Instance> getChildInstances() {
        ArrayList<Instance> childInstances = new ArrayList<>();
        for (ChildTask childTask : mTask.getChildTasks())
            childInstances.add(InstanceFactory.getInstance().getWeeklyInstance(childTask, mWeeklyRepetition));
        return childInstances;
    }

    public String getIntentKey() {
        return INTENT_KEY;
    }

    public int getIntentValue() {
        return getId();
    }

    public DateTime getDateTime() {
        return mWeeklyRepetition.getRepetitionDateTime();
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
                mInstanceRecord = PersistenceManger.getInstance().createWeeklyInstanceRecord(mId, mTask.getId(), mInstanceRecord.getId(), TimeStamp.getNow().getLong());
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
