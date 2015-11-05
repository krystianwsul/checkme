package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/12/2015.
 */
public class ChildTask extends Task {
    private final ArrayList<Task> mChildrenTasks = new ArrayList<>();

    protected ChildTask(int taskId, ArrayList<Integer> childTaskIds) {
        super(taskId);

        Assert.assertTrue(mTaskRecord.getParentTaskId() != null);

        for (Integer childTaskId : childTaskIds)
            mChildrenTasks.add(Task.getTask(childTaskId));
    }

    public ArrayList<Task> getChildTasks() {
        return mChildrenTasks;
    }

    public String getScheduleText(Context context) {
        return null;
    }
}
