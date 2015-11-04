package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyInstanceRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/2/2015.
 */
public abstract class WeeklyInstance implements Instance {
    private final Task mTask;
    private final WeeklyRepetition mWeeklyRepetition;

    private static final HashMap<Integer, WeeklyInstance> sWeeklyInstances = new HashMap<>();

    public static WeeklyInstance getWeeklyInstance(int weeklyInstanceId) {
        if (sWeeklyInstances.containsKey(weeklyInstanceId)) {
            return sWeeklyInstances.get(weeklyInstanceId);
        } else {
            PersistenceManger persistenceManger = PersistenceManger.getInstance();
            WeeklyInstanceRecord weeklyInstanceRecord = persistenceManger.getWeeklyInstanceRecord(weeklyInstanceId);
            WeeklyInstance weeklyInstance = new RealWeeklyInstance(Task.getTask(weeklyInstanceRecord.getTaskId()), weeklyInstanceRecord, WeeklyRepetition.getWeeklyRepetition(weeklyInstanceRecord.getWeeklyRepetitionId()));
            sWeeklyInstances.put(weeklyInstanceId, weeklyInstance);
            return weeklyInstance;
        }
    }

    public static WeeklyInstance getWeeklyInstance(Task task, WeeklyRepetition weeklyRepetition) {
        WeeklyInstance existingWeeklyInstance = getExistingWeeklyInstance(task.getId(), weeklyRepetition.getId());
        if (existingWeeklyInstance != null)
            return existingWeeklyInstance;

        WeeklyInstanceRecord weeklyInstanceRecord = PersistenceManger.getInstance().getWeeklyInstanceRecord(task.getId(), weeklyRepetition.getId());
        if (weeklyInstanceRecord != null) {
            RealWeeklyInstance realWeeklyInstance = new RealWeeklyInstance(task, weeklyInstanceRecord, weeklyRepetition);
            sWeeklyInstances.put(realWeeklyInstance.getId(), realWeeklyInstance);
            return realWeeklyInstance;
        }

        VirtualWeeklyInstance virtualWeeklyInstance = new VirtualWeeklyInstance(task, weeklyRepetition);
        sWeeklyInstances.put(virtualWeeklyInstance.getId(), virtualWeeklyInstance);
        return virtualWeeklyInstance;
    }

    private static WeeklyInstance getExistingWeeklyInstance(int taskId, int weeklyRepetitionId) {
        for (WeeklyInstance weeklyInstance : sWeeklyInstances.values())
            if (weeklyInstance.getTaskId() == taskId && weeklyInstance.getWeeklyRepetitionId() == weeklyRepetitionId)
                return weeklyInstance;
        return null;
    }

    public WeeklyInstance(Task task, WeeklyRepetition weeklyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);
        mTask = task;
        mWeeklyRepetition = weeklyRepetition;
    }

    public abstract int getId();

    public int getTaskId() {
        return mTask.getId();
    }

    public int getWeeklyRepetitionId() {
        return mWeeklyRepetition.getId();
    }

    public abstract boolean getDone();

    public String getName() {
        return mTask.getName();
    }

    public String getScheduleText(Context context) {
        return mWeeklyRepetition.getDateTime().getDisplayText(context);
    }
}
