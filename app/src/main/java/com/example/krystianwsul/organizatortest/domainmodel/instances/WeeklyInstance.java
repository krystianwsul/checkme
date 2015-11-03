package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyInstanceRecord;

import java.util.HashMap;

/**
 * Created by Krystian on 11/2/2015.
 */
public abstract class WeeklyInstance implements Instance {
    private static final HashMap<Integer, WeeklyInstance> sWeeklyInstances = new HashMap<>();

    public static WeeklyInstance getWeeklyInstance(int weeklyInstanceId) {
        if (sWeeklyInstances.containsKey(weeklyInstanceId)) {
            return sWeeklyInstances.get(weeklyInstanceId);
        } else {
            WeeklyInstance weeklyInstance = new RealWeeklyInstance(PersistenceManger.getInstance().getWeeklyInstanceRecord(weeklyInstanceId));
            sWeeklyInstances.put(weeklyInstanceId, weeklyInstance);
            return weeklyInstance;
        }
    }

    public static WeeklyInstance getWeeklyInstance(int taskId, int weeklyRepetitionId) {
        WeeklyInstance existingWeeklyInstance = getExistingWeeklyInstance(taskId, weeklyRepetitionId);
        if (existingWeeklyInstance != null)
            return existingWeeklyInstance;

        WeeklyInstanceRecord weeklyInstanceRecord = PersistenceManger.getInstance().getWeeklyInstanceRecord(taskId, weeklyRepetitionId);
        if (weeklyInstanceRecord != null) {
            RealWeeklyInstance realWeeklyInstance = new RealWeeklyInstance(weeklyInstanceRecord);
            sWeeklyInstances.put(realWeeklyInstance.getId(), realWeeklyInstance);
            return realWeeklyInstance;
        }

        VirtualWeeklyInstance virtualWeeklyInstance = new VirtualWeeklyInstance(taskId, weeklyRepetitionId);
        sWeeklyInstances.put(virtualWeeklyInstance.getId(), virtualWeeklyInstance);
        return virtualWeeklyInstance;
    }

    private static WeeklyInstance getExistingWeeklyInstance(int taskId, int weeklyRepetitionId) {
        for (WeeklyInstance weeklyInstance : sWeeklyInstances.values())
            if (weeklyInstance.getTaskId() == taskId && weeklyInstance.getWeeklyRepetitionId() == weeklyRepetitionId)
                return weeklyInstance;
        return null;
    }

    public abstract int getId();

    public abstract int getTaskId();

    public abstract int getWeeklyRepetitionId();

    public abstract boolean getDone();
}
