package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/12/2015.
 */
public class LeafTask extends Task {
    protected LeafTask(int taskId) {
        super(taskId);

        Assert.assertTrue(mTaskRecord.getParentTaskId() != null);

        Assert.assertTrue(mTaskRecord.getSingleScheduleId() == null);
        Assert.assertTrue(mTaskRecord.getWeeklyScheduleId() == null);
    }

    public ArrayList<Task> getChildTasks() {
        return null;
    }

    public Schedule getSchedule() {
        return null;
    }
}
