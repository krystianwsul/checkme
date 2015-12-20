package com.example.krystianwsul.organizator.domainmodel.instances;

import com.example.krystianwsul.organizator.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizator.domainmodel.tasks.Task;
import com.example.krystianwsul.organizator.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizator.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizator.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;

public class InstanceFactory {
    private static InstanceFactory sInstance;

    public static InstanceFactory getInstance() {
        if (sInstance == null)
            sInstance = new InstanceFactory();
        return sInstance;
    }

    private final ArrayList<Instance> mExistingInstances = new ArrayList<>();

    private InstanceFactory() {
        Collection<InstanceRecord> instanceRecords = PersistenceManger.getInstance().getInstanceRecords();
        Assert.assertTrue(instanceRecords != null);

        TaskFactory taskFactory = TaskFactory.getInstance();
        Assert.assertTrue(taskFactory != null);

        for (InstanceRecord instanceRecord : instanceRecords) {
            Task task = taskFactory.getTask(instanceRecord.getTaskId());
            Assert.assertTrue(task != null);

            Instance instance = new Instance(task, instanceRecord);
            mExistingInstances.add(instance);
        }
    }

    void addExistingInstance(Instance instance) {
        Assert.assertTrue(instance != null);
        mExistingInstances.add(instance);
    }

    public ArrayList<Instance> getExistingInstances() {
        return mExistingInstances;
    }

    public Instance getInstance(Task task, DateTime scheduleDateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDateTime != null);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Instance instance : mExistingInstances) {
            Assert.assertTrue(instance != null);
            if (instance.getTaskId() == task.getId() && instance.getScheduleDateTime().compareTo(scheduleDateTime) == 0)
                instances.add(instance);
        }

        if (!instances.isEmpty()) {
            Assert.assertTrue(instances.size() == 1);
            return instances.get(0);
        } else {
            return new Instance(task, scheduleDateTime);
        }
    }
}
