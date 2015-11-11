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

    private static final HashMap<Integer, DailyInstance> sDailyInstances = new HashMap<>();

    public static DailyInstance getDailyInstance(int dailyInstanceId) {
        Assert.assertTrue(sDailyInstances.containsKey(dailyInstanceId));
        return sDailyInstances.get(dailyInstanceId);
    }

    public static DailyInstance getDailyInstance(Task task, DailyRepetition dailyRepetition) {
        DailyInstance existingDailyInstance = getExistingDailyInstance(task.getId(), dailyRepetition.getId());
        if (existingDailyInstance != null)
            return existingDailyInstance;

        DailyInstanceRecord dailyInstanceRecord = PersistenceManger.getInstance().getDailyInstanceRecord(task.getId(), dailyRepetition.getId());
        if (dailyInstanceRecord != null) {
            RealDailyInstance realDailyInstance = new RealDailyInstance(task, dailyInstanceRecord, dailyRepetition);
            sDailyInstances.put(realDailyInstance.getId(), realDailyInstance);
            return realDailyInstance;
        }

        VirtualDailyInstance virtualDailyInstance = new VirtualDailyInstance(task, dailyRepetition);
        sDailyInstances.put(virtualDailyInstance.getId(), virtualDailyInstance);
        return virtualDailyInstance;
    }

    private static DailyInstance getExistingDailyInstance(int taskId, int dailyRepetitionId) {
        for (DailyInstance dailyInstance : sDailyInstances.values())
            if (dailyInstance.getTaskId() == taskId && dailyInstance.getDailyRepetitionId() == dailyRepetitionId)
                return dailyInstance;
        return null;
    }

    public DailyInstance(Task task, DailyRepetition dailyRepetition) {
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
        return getDateTime().getDisplayText(context);
    }

    public ArrayList<Instance> getChildInstances() {
        ArrayList<Instance> childInstances = new ArrayList<>();
        for (ChildTask childTask : mTask.getChildTasks())
            childInstances.add(getDailyInstance(childTask, mDailyRepetition));
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
