package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyInstanceRecord;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyInstance implements Instance {
    private final Task mTask;
    private final WeeklyRepetition mWeeklyRepetition;

    private WeeklyInstanceRecord mWeeklyInstanceRecord;

    private final int mId;

    private static int sVirtualWeeklyInstanceCount = 0;

    public static final String INTENT_KEY = "weeklyInstanceId";

    WeeklyInstance(Task task, WeeklyRepetition weeklyRepetition, WeeklyInstanceRecord weeklyInstanceRecord) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);
        Assert.assertTrue(weeklyInstanceRecord != null);

        mTask = task;
        mWeeklyRepetition = weeklyRepetition;

        mWeeklyInstanceRecord = weeklyInstanceRecord;

        mId = mWeeklyInstanceRecord.getId();
    }

    WeeklyInstance(Task task, WeeklyRepetition weeklyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);

        mTask = task;
        mWeeklyRepetition = weeklyRepetition;

        mWeeklyInstanceRecord = null;

        mId = PersistenceManger.getInstance().getMaxWeeklyInstanceId() + ++sVirtualWeeklyInstanceCount;
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
            childInstances.add(WeeklyInstanceFactory.getInstance().getWeeklyInstance(childTask, mWeeklyRepetition));
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
        if (mWeeklyInstanceRecord == null)
            return null;

        Long done = mWeeklyInstanceRecord.getDone();
        if (done != null)
            return new TimeStamp(done);
        else
            return null;
    }

    public void setDone(boolean done) {
        if (mWeeklyInstanceRecord == null) {
            if (done)
                mWeeklyInstanceRecord = PersistenceManger.getInstance().createWeeklyInstanceRecord(mId, mTask.getId(), mWeeklyInstanceRecord.getId(), TimeStamp.getNow().getLong());
            else
                return;
        } else {
            if (done)
                mWeeklyInstanceRecord.setDone(TimeStamp.getNow().getLong());
            else
                mWeeklyInstanceRecord.setDone(null);
        }
    }
}
