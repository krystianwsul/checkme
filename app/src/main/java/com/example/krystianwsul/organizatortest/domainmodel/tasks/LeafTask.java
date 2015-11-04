package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/12/2015.
 */
public class LeafTask extends Task implements ChildTask {
    protected LeafTask(int taskId) {
        super(taskId);

        Assert.assertTrue(mTaskRecord.getParentTaskId() != null);

        Assert.assertTrue(mTaskRecord.getSingleScheduleId() == null);
        Assert.assertTrue(mTaskRecord.getWeeklyScheduleId() == null);
    }

    public ArrayList<TaskTest> getChildTasks() {
        return null;
    }

    public String getScheduleText(Context context) {
        return null;
    }
}
