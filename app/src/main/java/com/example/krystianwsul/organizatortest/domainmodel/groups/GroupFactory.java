package com.example.krystianwsul.organizatortest.domainmodel.groups;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Krystian on 11/11/2015.
 */
public class GroupFactory {
    private static GroupFactory sInstance;

    private HashMap<DateTime, Group> mGroups = new HashMap<>();

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
            DateTime dateTime = instance.getDateTime();
            if (mGroups.containsKey(dateTime)) {
                mGroups.get(dateTime).addInstance(instance);
            } else {
                Group group = new Group(dateTime.getDate(), dateTime.getTime().getTimeByDay(dateTime.getDate().getDayOfWeek()));
                group.addInstance(instance);
                mGroups.put(dateTime, group);
            }
        }
    }

    public Collection<Group> getGroups() {
        return mGroups.values();
    }
}
