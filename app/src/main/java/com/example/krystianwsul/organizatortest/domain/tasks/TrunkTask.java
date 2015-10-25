package com.example.krystianwsul.organizatortest.domain.tasks;

import com.example.krystianwsul.organizatortest.domain.Completion;
import com.example.krystianwsul.organizatortest.domain.instances.TopInstance;
import com.example.krystianwsul.organizatortest.domain.instances.TrunkInstance;
import com.example.krystianwsul.organizatortest.timing.DateTime;
import com.example.krystianwsul.organizatortest.timing.TimeStamp;
import com.example.krystianwsul.organizatortest.timing.schedules.Schedule;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 10/13/2015.
 */
public class TrunkTask implements TopTask, Task, ParentTask {
    private String mName;
    private Schedule mSchedule;

    private ArrayList<ChildTask> mChildren = new ArrayList<>();

    private HashMap<DateTime, Completion> mEntries = new HashMap<>();

    public TrunkTask(String name, Schedule schedule) {
        Assert.assertTrue(name != null);
        Assert.assertTrue(!name.isEmpty());
        Assert.assertTrue(schedule != null);

        mName = name;
        mSchedule = schedule;
    }

    public String getName() {
        return mName;
    }

    public Schedule getSchedule() {
        return mSchedule;
    }

    public void addChild(ChildTask child) {
        Assert.assertTrue(child != null);
        mChildren.add(child);
    }

    public ArrayList<ChildTask> getChildren() {
        return mChildren;
    }

    public ArrayList<TopInstance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        ArrayList<DateTime> dateTimes = mSchedule.getDateTimes(startTimeStamp, endTimeStamp);

        ArrayList<TopInstance> ret = new ArrayList<>();
        for (DateTime dateTime : dateTimes) {
            ret.add(new TrunkInstance(this, dateTime));
        }

        return ret;
    }
}
