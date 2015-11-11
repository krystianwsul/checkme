package com.example.krystianwsul.organizatortest.domainmodel.groups;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by Krystian on 11/11/2015.
 */
public class GroupFactory {
    private static GroupFactory sInstance;

    private TreeMap<TimeStamp, Group> mGroups = new TreeMap<>();

    public static GroupFactory getInstance() {
        if (sInstance == null)
            sInstance = new GroupFactory();
        return sInstance;
    }

    private GroupFactory() {
        Collection<RootTask> rootTasks = TaskFactory.getInstance().getRootTasks();

        Calendar tomorrowCalendar = Calendar.getInstance();
        tomorrowCalendar.add(Calendar.DATE, 1);
        Date tomorrowDate = new Date(tomorrowCalendar);

        final ArrayList<Instance> instances = new ArrayList<>();
        for (RootTask rootTask : rootTasks)
            instances.addAll(rootTask.getInstances(null, new TimeStamp(tomorrowDate, new HourMinute(0, 0))));

        for (Instance instance : instances) {
            TimeStamp timeStamp = instance.getDateTime().getTimeStamp();
            if (mGroups.containsKey(timeStamp)) {
                mGroups.get(timeStamp).addInstance(instance);
            } else {
                Group group = new Group(timeStamp);
                group.addInstance(instance);
                mGroups.put(timeStamp, group);
            }
        }
    }

    public Collection<Group> getGroups() {
        return mGroups.values();
    }

    public Group getGroup(TimeStamp timeStamp) {
        Assert.assertTrue(mGroups.containsKey(timeStamp));
        return mGroups.get(timeStamp);
    }
}
