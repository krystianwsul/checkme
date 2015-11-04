package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/12/2015.
 */
public class BranchTask extends Task implements ChildTask {
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

    public ArrayList<TaskTest> getChildTasks() {
        ArrayList<TaskTest> childTasks = new ArrayList<>();
        for (Task task : mChildrenTasks)
            childTasks.add(task);
        return childTasks;
    }

    public String getScheduleText(Context context) {
        return null;
    }
}
