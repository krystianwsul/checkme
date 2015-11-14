package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyInstanceRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 11/2/2015.
 */
public abstract class DailyInstance implements Instance {
    private final Task mTask;
    private final DailyRepetition mDailyRepetition;

    public static final String INTENT_KEY = "dailyInstanceId";

    protected DailyInstance(Task task, DailyRepetition dailyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);
        mTask = task;
        mDailyRepetition = dailyRepetition;
    }

    public abstract int getId();

    public int getTaskId() {
        return mTask.getId();
    }

    public int getDailyRepetitionId() {
        return mDailyRepetition.getId();
    }

    public abstract boolean getDone();

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
}
