package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/13/2015.
 */
public class RootTask extends Task {
    private final ArrayList<Task> mChildrenTasks = new ArrayList<>();

    private final Schedule mSchedule;

    protected RootTask(int taskId, ArrayList<Integer> childTaskIds) {
        super(taskId);

        Assert.assertTrue(mTaskRecord.getParentTaskId() == null);

        for (Integer childTaskId : childTaskIds)
            mChildrenTasks.add(Task.getTask(childTaskId));

        mSchedule = Schedule.getSchedule(taskId);
        Assert.assertTrue(mSchedule != null);
    }

    public ArrayList<Task> getChildTasks() {
        return mChildrenTasks;
    }

    public String getScheduleText(Context context) {
        return mSchedule.getTaskText(context);
    }

    public ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        return mSchedule.getInstances(this, startTimeStamp, endTimeStamp);
    }
}
