package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.util.ArrayList;

public class RootTask extends Task {
    private Schedule mSchedule;

    RootTask(TaskRecord taskRecord) {
        super(taskRecord);

        Assert.assertTrue(mTaskRecord.getParentTaskId() == null);
    }

    public void setSchedule(Schedule schedule) {
        Assert.assertTrue(schedule != null);
        mSchedule = schedule;
    }

    public String getScheduleText(Context context) {
        return mSchedule.getTaskText(context);
    }

    private Schedule getSchedule() {
        Assert.assertTrue(mSchedule != null);
        return mSchedule;
    }

    public ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        Assert.assertTrue(mSchedule != null);
        Assert.assertTrue(endTimeStamp != null);

        return mSchedule.getInstances(startTimeStamp, endTimeStamp);
    }

    public RootTask getRootTask() {
        return this;
    }

    public Task getParentTask() {
        return null;
    }

    public boolean isRootTask() {
        return true;
    }
}
