package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/12/2015.
 */
public class BranchTask extends Task {
    private final ArrayList<Task> mChildrenTasks = new ArrayList<>();

    protected BranchTask(int taskId, ArrayList<Integer> childTaskIds) {
        super(taskId);

        Assert.assertTrue(mTaskRecord.getParentTaskId() != null);

        Assert.assertTrue(!childTaskIds.isEmpty());
        for (Integer childTaskId : childTaskIds)
            mChildrenTasks.add(Task.getTask(childTaskId));

        Assert.assertTrue(mTaskRecord.getSingleScheduleId() == null);
        Assert.assertTrue(mTaskRecord.getWeeklyScheduleId() == null);
    }

    public ArrayList<Task> getChildTasks() {
        return mChildrenTasks;
    }

    public String getScheduleText(Context context) {
        return null;
    }
}
