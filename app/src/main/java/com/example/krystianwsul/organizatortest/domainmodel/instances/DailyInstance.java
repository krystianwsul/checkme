package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyInstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 11/2/2015.
 */
public class DailyInstance implements Instance {
    private final Task mTask;
    private final DailyRepetition mDailyRepetition;

    private final DailyInstanceRecord mDailyInstanceRecord;

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
}
