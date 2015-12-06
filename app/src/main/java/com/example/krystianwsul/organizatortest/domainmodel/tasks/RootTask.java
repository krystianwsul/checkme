package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.util.ArrayList;

public class RootTask extends Task {
    Schedule mSchedule;

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

    public ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        Assert.assertTrue(mSchedule != null);
        Assert.assertTrue(endTimeStamp != null);

        return mSchedule.getInstances(startTimeStamp, endTimeStamp);
    }

    public RootTask getRootTask() {
        return this;
    }

    public boolean isRootTask() {
        return true;
    }

    public boolean isMutable() {
        return mSchedule.isMutable();
    }

    public boolean current() {
        Assert.assertTrue(mSchedule != null);
        return mSchedule.current();
    }
}
