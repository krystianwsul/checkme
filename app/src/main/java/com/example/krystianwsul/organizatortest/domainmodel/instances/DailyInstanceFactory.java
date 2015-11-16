package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyInstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public class DailyInstanceFactory {
    private static DailyInstanceFactory sInstance;

    public static DailyInstanceFactory getInstance() {
        if (sInstance == null)
            sInstance = new DailyInstanceFactory();
        return sInstance;
    }

    private DailyInstanceFactory() {}

    private final HashMap<Integer, DailyInstance> mDailyInstances = new HashMap<>();

    public DailyInstance getDailyInstance(int dailyInstanceId) {
        Assert.assertTrue(mDailyInstances.containsKey(dailyInstanceId));
        return mDailyInstances.get(dailyInstanceId);
    }

    public DailyInstance getDailyInstance(Task task, DailyRepetition dailyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);

        DailyInstance existingDailyInstance = getExistingDailyInstance(task.getId(), dailyRepetition.getId());
        if (existingDailyInstance != null)
            return existingDailyInstance;

        DailyInstance dailyInstance = new DailyInstance(task, dailyRepetition);
        Assert.assertTrue(dailyInstance != null);
        mDailyInstances.put(dailyInstance.getId(), dailyInstance);
        return dailyInstance;
    }

    private DailyInstance getExistingDailyInstance(int taskId, int dailyRepetitionId) {
        for (DailyInstance dailyInstance : mDailyInstances.values())
            if (dailyInstance.getTaskId() == taskId && dailyInstance.getDailyRepetitionId() == dailyRepetitionId)
                return dailyInstance;
        return null;
    }

    private DailyInstance createDailyInstance(Task task, DailyRepetition dailyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);

        DailyInstanceRecord dailyInstanceRecord = PersistenceManger.getInstance().getDailyInstanceRecord(task.getId(), dailyRepetition.getId());
        if (dailyInstanceRecord != null)
            return new DailyInstance(task, dailyRepetition, dailyInstanceRecord);
        else
            return new DailyInstance(task, dailyRepetition);
    }
}
