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

    public static SingleInstance getSingleInstance(int taskId) {
        if (sSingleInstances.containsKey(taskId)) {
            return sSingleInstances.get(taskId);
        } else {
            PersistenceManger persistenceManger = PersistenceManger.getInstance();
            SingleInstanceRecord singleInstanceRecord = persistenceManger.getSingleInstanceRecord(taskId);
            SingleInstance singleInstance = new RealSingleInstance(Task.getTask(singleInstanceRecord.getTaskId()), singleInstanceRecord);
            sSingleInstances.put(taskId, singleInstance);
            return singleInstance;
        }
    }

    public static SingleInstance getSingleInstance(Task task) {
        SingleInstance existingSingleInstance = sSingleInstances.get(task.getId());
        if (existingSingleInstance != null)
            return existingSingleInstance;

        SingleInstanceRecord singleInstanceRecord = PersistenceManger.getInstance().getSingleInstanceRecord(task.getId());
        if (singleInstanceRecord != null) {
            RealSingleInstance realSingleInstance = new RealSingleInstance(task, singleInstanceRecord);
            sSingleInstances.put(realSingleInstance.getTaskId(), realSingleInstance);
            return realSingleInstance;
        }

        VirtualSingleInstance virtualSingleInstance = new VirtualSingleInstance(task);
        sSingleInstances.put(virtualSingleInstance.getTaskId(), virtualSingleInstance);
        return virtualSingleInstance;
    }

    public SingleInstance(Task task) {
        Assert.assertTrue(task != null);
        mTask = task;
    }

    public int getTaskId() {
        return mTask.getId();
    }

    public abstract boolean getDone();

    public String getName() {
        return mTask.getName();
    }
}
