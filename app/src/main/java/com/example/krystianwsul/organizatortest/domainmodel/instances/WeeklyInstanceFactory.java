package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyInstanceRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public class WeeklyInstanceFactory {
    private static WeeklyInstanceFactory sInstance;

    public static WeeklyInstanceFactory getInstance() {
        if (sInstance == null)
            sInstance = new WeeklyInstanceFactory();
        return sInstance;
    }

    private WeeklyInstanceFactory() {}

    private final HashMap<Integer, WeeklyInstance> mWeeklyInstances = new HashMap<>();

    public WeeklyInstance getWeeklyInstance(int weeklyInstanceId) {
        Assert.assertTrue(mWeeklyInstances.containsKey(weeklyInstanceId));
        return mWeeklyInstances.get(weeklyInstanceId);
    }

    public WeeklyInstance getWeeklyInstance(Task task, WeeklyRepetition weeklyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);

        WeeklyInstance existingWeeklyInstance = getExistingWeeklyInstance(task.getId(), weeklyRepetition.getId());
        if (existingWeeklyInstance != null)
            return existingWeeklyInstance;

        WeeklyInstance weeklyInstance = new WeeklyInstance(task, weeklyRepetition);
        Assert.assertTrue(weeklyInstance != null);
        mWeeklyInstances.put(weeklyInstance.getId(), weeklyInstance);
        return weeklyInstance;
    }

    private WeeklyInstance getExistingWeeklyInstance(int taskId, int weeklyRepetitionId) {
        for (WeeklyInstance weeklyInstance : mWeeklyInstances.values())
            if (weeklyInstance.getTaskId() == taskId && weeklyInstance.getWeeklyRepetitionId() == weeklyRepetitionId)
                return weeklyInstance;
        return null;
    }

    private WeeklyInstance createWeeklyInstance(Task task, WeeklyRepetition weeklyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);

        WeeklyInstanceRecord weeklyInstanceRecord = PersistenceManger.getInstance().getWeeklyInstanceRecord(task.getId(), weeklyRepetition.getId());
        if (weeklyInstanceRecord != null)
            return new WeeklyInstance(task, weeklyRepetition, weeklyInstanceRecord);
        else
            return new WeeklyInstance(task, weeklyRepetition);
    }
}
