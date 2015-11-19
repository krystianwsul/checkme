package com.example.krystianwsul.organizatortest.domainmodel.groups;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by Krystian on 11/11/2015.
 */
public class GroupFactory {
    private static GroupFactory sInstance;

    private TreeMap<TimeStamp, Group> mDoneGroups = new TreeMap<>();
    private TreeMap<TimeStamp, Group> mNotDoneGroups = new TreeMap<>();

    public static GroupFactory getInstance() {
        if (sInstance == null)
            sInstance = new GroupFactory();
        return sInstance;
    }

    public static void refresh() {
        sInstance = null;
    }

    private GroupFactory() {
        Collection<RootTask> rootTasks = TaskFactory.getInstance().getRootTasks();

        Calendar tomorrowCalendar = Calendar.getInstance();
        tomorrowCalendar.add(Calendar.DATE, 1);
        Date tomorrowDate = new Date(tomorrowCalendar);

        ArrayList<Instance> instances = new ArrayList<>();
        for (RootTask rootTask : rootTasks)
            instances.addAll(rootTask.getInstances(null, new TimeStamp(tomorrowDate, new HourMinute(0, 0))));

        ArrayList<Instance> doneInstances = new ArrayList<>();
        ArrayList<Instance> notDoneInstances = new ArrayList<>();
        for (Instance instance : instances) {
            if (instance.getDone() != null)
                doneInstances.add(instance);
            else
                notDoneInstances.add(instance);
        }

        for (Instance instance : doneInstances) {
            Group group = new Group(instance.getDone());
            group.addInstance(instance);
            mDoneGroups.put(group.getTimeStamp(), group);
        }

        for (Instance instance : notDoneInstances) {
            TimeStamp timeStamp = instance.getDateTime().getTimeStamp();
            if (mNotDoneGroups.containsKey(timeStamp)) {
                mNotDoneGroups.get(timeStamp).addInstance(instance);
            } else {
                Group group = new Group(timeStamp);
                group.addInstance(instance);
                mNotDoneGroups.put(timeStamp, group);
            }
        }
    }

    public Collection<Group> getGroups() {
        ArrayList<Group> groups = new ArrayList<>();
        groups.addAll(mDoneGroups.values());
        groups.addAll(mNotDoneGroups.values());
        return groups;
    }
}