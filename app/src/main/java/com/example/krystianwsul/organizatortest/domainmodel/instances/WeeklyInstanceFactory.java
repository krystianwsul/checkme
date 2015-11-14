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
        WeeklyInstance existingWeeklyInstance = getExistingWeeklyInstance(task.getId(), weeklyRepetition.getId());
        if (existingWeeklyInstance != null)
            return existingWeeklyInstance;

        WeeklyInstanceRecord weeklyInstanceRecord = PersistenceManger.getInstance().getWeeklyInstanceRecord(task.getId(), weeklyRepetition.getId());
        if (weeklyInstanceRecord != null) {
            RealWeeklyInstance realWeeklyInstance = new RealWeeklyInstance(task, weeklyInstanceRecord, weeklyRepetition);
            mWeeklyInstances.put(realWeeklyInstance.getId(), realWeeklyInstance);
            return realWeeklyInstance;
        }

        VirtualWeeklyInstance virtualWeeklyInstance = new VirtualWeeklyInstance(task, weeklyRepetition);
        mWeeklyInstances.put(virtualWeeklyInstance.getId(), virtualWeeklyInstance);
        return virtualWeeklyInstance;
    }

    private WeeklyInstance getExistingWeeklyInstance(int taskId, int weeklyRepetitionId) {
        for (WeeklyInstance weeklyInstance : mWeeklyInstances.values())
            if (weeklyInstance.getTaskId() == taskId && weeklyInstance.getWeeklyRepetitionId() == weeklyRepetitionId)
                return weeklyInstance;
        return null;
    }
}
