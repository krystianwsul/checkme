package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleInstanceRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/2/2015.
 */
public abstract class SingleInstance implements Instance {
    protected final Task mTask;

    private static final HashMap<Integer, SingleInstance> sSingleInstances = new HashMap<>();

    public static SingleInstance getSingleInstance(int singleInstanceId) {
        if (sSingleInstances.containsKey(singleInstanceId)) {
            return sSingleInstances.get(singleInstanceId);
        } else {
            PersistenceManger persistenceManger = PersistenceManger.getInstance();
            SingleInstanceRecord singleInstanceRecord = persistenceManger.getSingleInstanceRecord(singleInstanceId);
            SingleInstance singleInstance = new RealSingleInstance(Task.getTask(singleInstanceRecord.getTaskId()), singleInstanceRecord);
            sSingleInstances.put(singleInstanceId, singleInstance);
            return singleInstance;
        }
    }

    public static SingleInstance getSingleInstance(Task task, int singleScheduleId) {
        SingleInstance existingSingleInstance = getExistingSingleInstance(task.getId(), singleScheduleId);
        if (existingSingleInstance != null)
            return existingSingleInstance;

        SingleInstanceRecord singleInstanceRecord = PersistenceManger.getInstance().getSingleInstanceRecord(task.getId(), singleScheduleId);
        if (singleInstanceRecord != null) {
            RealSingleInstance realSingleInstance = new RealSingleInstance(task, singleInstanceRecord);
            sSingleInstances.put(realSingleInstance.getId(), realSingleInstance);
            return realSingleInstance;
        }

        VirtualSingleInstance virtualSingleInstance = new VirtualSingleInstance(task, singleScheduleId);
        sSingleInstances.put(virtualSingleInstance.getId(), virtualSingleInstance);
        return virtualSingleInstance;
    }

    private static SingleInstance getExistingSingleInstance(int taskId, int singleScheduleId) {
        for (SingleInstance singleInstance : sSingleInstances.values())
            if (singleInstance.getTaskId() == taskId && singleInstance.getSingleScheduleId() == singleScheduleId)
                return singleInstance;
        return null;
    }

    public SingleInstance(Task task) {
        Assert.assertTrue(task != null);
        mTask = task;
    }

    public abstract int getId();

    public int getTaskId() {
        return mTask.getId();
    }

    public abstract int getSingleScheduleId();

    public abstract boolean getDone();

    public String getName() {
        return mTask.getName();
    }
}
