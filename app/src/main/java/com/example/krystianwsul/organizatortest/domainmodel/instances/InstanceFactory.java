package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

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

    private final ArrayList<Instance> mInstances = new ArrayList<>();

    private InstanceFactory() {
        Collection<InstanceRecord> instanceRecords = PersistenceManger.getInstance().getInstanceRecords();
        Assert.assertTrue(instanceRecords != null);

        TaskFactory taskFactory = TaskFactory.getInstance();
        Assert.assertTrue(taskFactory != null);

        for (InstanceRecord instanceRecord : instanceRecords) {
            Task task = taskFactory.getTask(instanceRecord.getTaskId());
            Assert.assertTrue(task != null);

            Instance instance = new Instance(task, instanceRecord);
            mInstances.add(instance);
        }
    }

    public Instance getInstance(Task task, DateTime scheduleDateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDateTime != null);
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp()));

        ArrayList<Instance> instances = new ArrayList<>();
        for (Instance instance : mInstances) {
            Assert.assertTrue(instance != null);
            if (instance.getTaskId() == task.getId() && instance.getScheduleDateTime().compareTo(scheduleDateTime) == 0)
                instances.add(instance);
        }

        if (!instances.isEmpty()) {
            Assert.assertTrue(instances.size() == 1);
            return instances.get(0);
        } else {
            Instance instance = new Instance(task, scheduleDateTime);
            mInstances.add(instance);
            return instance;
        }
    }
}
