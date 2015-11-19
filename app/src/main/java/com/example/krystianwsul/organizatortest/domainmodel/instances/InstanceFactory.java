package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/18/2015.
 */
public class InstanceFactory {
    private static InstanceFactory sInstance;

    public static InstanceFactory getInstance() {
        if (sInstance == null)
            sInstance = new InstanceFactory();
        return sInstance;
    }

    private InstanceFactory() {}

    private final HashMap<Integer, SingleInstance> mSingleInstances = new HashMap<>();
    private final HashMap<Integer, DailyInstance> mDailyInstances = new HashMap<>();
    private final HashMap<Integer, WeeklyInstance> mWeeklyInstances = new HashMap<>();

    private final HashMap<Integer, Instance> mInstances = new HashMap<>();

    private static int mVirtualInstanceCount = 0;

    private int getNextInstanceId() {
        return PersistenceManger.getInstance().getMaxInstanceId() + ++mVirtualInstanceCount;
    }

    public Instance getInstance(int instanceId) {
        Assert.assertTrue(mInstances.containsKey(instanceId));
        return mInstances.get(instanceId);
    }

    public SingleInstance getSingleInstance(Task task, SingleRepetition singleRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(singleRepetition != null);

        SingleInstance existingSingleInstance = getExistingSingleInstance(task.getId(), singleRepetition.getRootTaskId());
        if (existingSingleInstance != null)
            return existingSingleInstance;

        SingleInstance singleInstance = createSingleInstance(task, singleRepetition);
        Assert.assertTrue(singleInstance != null);
        mSingleInstances.put(singleInstance.getId(), singleInstance);
        mInstances.put(singleInstance.getId(), singleInstance);
        return singleInstance;
    }

    private SingleInstance getExistingSingleInstance(int taskId, int rootTaskId) {
        for (SingleInstance singleInstance : mSingleInstances.values())
            if (singleInstance.getTaskId() == taskId && singleInstance.getRootTaskId() == rootTaskId)
                return singleInstance;
        return null;
    }

    private SingleInstance createSingleInstance(Task task, SingleRepetition singleRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(singleRepetition != null);

        InstanceRecord instanceRecord = PersistenceManger.getInstance().getSingleInstanceRecord(task.getId());
        if (instanceRecord != null)
            return new SingleInstance(task, singleRepetition, instanceRecord);
        else
            return new SingleInstance(task, singleRepetition, getNextInstanceId());
    }

    public DailyInstance getDailyInstance(Task task, DailyRepetition dailyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dailyRepetition != null);

        DailyInstance existingDailyInstance = getExistingDailyInstance(task.getId(), dailyRepetition.getId());
        if (existingDailyInstance != null)
            return existingDailyInstance;

        DailyInstance dailyInstance = createDailyInstance(task, dailyRepetition);
        Assert.assertTrue(dailyInstance != null);
        mDailyInstances.put(dailyInstance.getId(), dailyInstance);
        mInstances.put(dailyInstance.getId(), dailyInstance);
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

        InstanceRecord instanceRecord = PersistenceManger.getInstance().getDailyInstanceRecord(task.getId(), dailyRepetition.getId());
        if (instanceRecord != null)
            return new DailyInstance(task, dailyRepetition, instanceRecord);
        else
            return new DailyInstance(task, dailyRepetition, getNextInstanceId());
    }

    public WeeklyInstance getWeeklyInstance(Task task, WeeklyRepetition weeklyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);

        WeeklyInstance existingWeeklyInstance = getExistingWeeklyInstance(task.getId(), weeklyRepetition.getId());
        if (existingWeeklyInstance != null)
            return existingWeeklyInstance;

        WeeklyInstance weeklyInstance = createWeeklyInstance(task, weeklyRepetition);
        Assert.assertTrue(weeklyInstance != null);
        mWeeklyInstances.put(weeklyInstance.getId(), weeklyInstance);
        mInstances.put(weeklyInstance.getId(), weeklyInstance);
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

        InstanceRecord instanceRecord = PersistenceManger.getInstance().getWeeklyInstanceRecord(task.getId(), weeklyRepetition.getId());
        if (instanceRecord != null)
            return new WeeklyInstance(task, weeklyRepetition, instanceRecord);
        else
            return new WeeklyInstance(task, weeklyRepetition, getNextInstanceId());
    }
}
