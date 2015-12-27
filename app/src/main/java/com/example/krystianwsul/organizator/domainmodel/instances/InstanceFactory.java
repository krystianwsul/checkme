package com.example.krystianwsul.organizator.domainmodel.instances;

import android.util.Log;

import com.example.krystianwsul.organizator.domainmodel.dates.Date;
import com.example.krystianwsul.organizator.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizator.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizator.domainmodel.tasks.Task;
import com.example.krystianwsul.organizator.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizator.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizator.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizator.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;

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

    private ArrayList<Instance> getRootInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        Assert.assertTrue(endTimeStamp != null);
        Assert.assertTrue(startTimeStamp == null || startTimeStamp.compareTo(endTimeStamp) < 0);

        HashSet<Instance> allInstances = new HashSet<>();
        allInstances.addAll(mExistingInstances);

        Collection<Task> tasks = TaskFactory.getInstance().getTasks();

        for (Task task : tasks)
            allInstances.addAll(task.getInstances(startTimeStamp, endTimeStamp));

        ArrayList<Instance> rootInstances = new ArrayList<>();
        for (Instance instance : allInstances)
            if (instance.isRootInstance())
                rootInstances.add(instance);

        return rootInstances;
    }

    public ArrayList<Instance> getCurrentInstances() {
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.DATE, 2);
        Date endDate = new Date(endCalendar);

        return getRootInstances(null, new TimeStamp(endDate, new HourMinute(0, 0)));
    }

    public ArrayList<Instance> getNotificationInstances() {
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.MINUTE, 1);

        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ArrayList<Instance> rootInstances = getRootInstances(null, endTimeStamp);

        ArrayList<Instance> notificationInstances = new ArrayList<>();
        for (Instance instance : rootInstances)
            if (instance.getDone() == null && !instance.getNotified())
                notificationInstances.add(instance);
        return notificationInstances;
    }

    public ArrayList<Instance> getCurrentInstances(TimeStamp timeStamp) {
        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ArrayList<Instance> rootInstances = getRootInstances(timeStamp, endTimeStamp);

        ArrayList<Instance> currentInstances = new ArrayList<>();
        for (Instance instance : rootInstances)
            if (instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                currentInstances.add(instance);

        return currentInstances;
    }

    public ArrayList<Instance> getShownInstances() {
        ArrayList<Instance> shownInstances = new ArrayList<>();

        for (Instance instance : mExistingInstances)
            if (instance.getNotificationShown())
                shownInstances.add(instance);

        return shownInstances;
    }
}
