package com.example.krystianwsul.organizatortest.domain.tasks;

import com.example.krystianwsul.organizatortest.domain.Completion;
import com.example.krystianwsul.organizatortest.domain.instances.StubInstance;
import com.example.krystianwsul.organizatortest.domain.instances.TopInstance;
import com.example.krystianwsul.organizatortest.timing.DateTime;
import com.example.krystianwsul.organizatortest.timing.TimeStamp;
import com.example.krystianwsul.organizatortest.timing.schedules.Schedule;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 10/13/2015.
 */
public class StubTask implements TopTask, Task, TipTask {
    private String mName;
    private Schedule mSchedule;

    private HashMap<DateTime, Completion> mEntries = new HashMap<>();

    public StubTask(String name, Schedule schedule)
    {
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

    public ArrayList<TopInstance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        ArrayList<DateTime> dateTimes = mSchedule.getDateTimes(startTimeStamp, endTimeStamp);

        ArrayList<TopInstance> ret = new ArrayList<>();
        for (DateTime dateTime : dateTimes) {
            ret.add(new StubInstance(this, dateTime));
        }

        return ret;
    }
}
