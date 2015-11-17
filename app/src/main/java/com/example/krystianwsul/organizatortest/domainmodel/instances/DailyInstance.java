package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyInstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.security.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 11/2/2015.
 */
public class DailyInstance implements Instance {
    private final Task mTask;
    private final DailyRepetition mDailyRepetition;

    private DailyInstanceRecord mDailyInstanceRecord;

    private final int mId;

    private static int sVirtualDailyInstanceCount = 0;

    public static final String INTENT_KEY = "dailyInstanceId";

    DailyInstance(Task task, DailyRepetition dailyRepetition, DailyInstanceRecord dailyInstanceRecord) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);
        Assert.assertTrue(dailyInstanceRecord != null);

        mTask = task;
        mDailyRepetition = dailyRepetition;

        mDailyInstanceRecord = dailyInstanceRecord;

        mId = mDailyInstanceRecord.getId();
    }

    DailyInstance(Task task, DailyRepetition dailyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);

        mTask = task;
        mDailyRepetition = dailyRepetition;

        mDailyInstanceRecord = null;

        mId = PersistenceManger.getInstance().getMaxDailyInstanceId() + ++sVirtualDailyInstanceCount;
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
            childInstances.add(DailyInstanceFactory.getInstance().getDailyInstance(childTask, mDailyRepetition));
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
        if (mDailyInstanceRecord == null)
            return null;

        Long done = mDailyInstanceRecord.getDone();
        if (done != null)
            return new TimeStamp(done);
        else
            return null;
    }

    public void setDone(boolean done) {
        if (mDailyInstanceRecord == null) {
            if (done)
                mDailyInstanceRecord = PersistenceManger.getInstance().createDailyInstanceRecord(mId, mTask.getId(), mDailyRepetition.getId(), TimeStamp.getNow().getLong());
            else
                return;
        } else {
            if (done)
                mDailyInstanceRecord.setDone(TimeStamp.getNow().getLong());
            else
                mDailyInstanceRecord.setDone(null);
        }
    }
}
