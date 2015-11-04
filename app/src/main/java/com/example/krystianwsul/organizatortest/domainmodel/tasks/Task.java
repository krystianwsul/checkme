package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 10/13/2015.
 */
public abstract class Task implements TaskTest {
    protected final TaskRecord mTaskRecord;

    private static final HashMap<Integer, Task> sTasks = new HashMap<>();

    public static Task getTask(int taskId) {
        if (sTasks.containsKey(taskId)) {
            return sTasks.get(taskId);
        } else {
            Task task = createTask(taskId);
            sTasks.put(taskId, task);
            return task;
        }
    }

    public static ArrayList<RootTask> getRootTasks() {
        ArrayList<Integer> rootTaskIds = PersistenceManger.getInstance().getTaskIds(null);
        Assert.assertTrue(!rootTaskIds.isEmpty());

        ArrayList<RootTask> rootTasks = new ArrayList<>();
        for (Integer rootTaskId : rootTaskIds)
            rootTasks.add((RootTask) getTask(rootTaskId));
        return rootTasks;
    }

    private static Task createTask(int taskId) {
        PersistenceManger persistenceManger = PersistenceManger.getInstance();
        TaskRecord taskRecord = persistenceManger.getTaskRecord(taskId);
        Assert.assertTrue(taskRecord != null);

        ArrayList<Integer> childTaskIds = persistenceManger.getTaskIds(taskId);

        if (taskRecord.getParentTaskId() == null) {
            if (childTaskIds.isEmpty())
                return new StubTask(taskId);
            else
                return new TrunkTask(taskId, childTaskIds);
        } else {
            if (childTaskIds.isEmpty())
                return new LeafTask(taskId);
            else
                return new BranchTask(taskId, childTaskIds);
        }
    }

    protected Task(int taskId) {
        mTaskRecord = PersistenceManger.getInstance().getTaskRecord(taskId);
        Assert.assertTrue(mTaskRecord != null);
    }

    public String getName() {
        return mTaskRecord.getName();
    }

    public abstract ArrayList<TaskTest> getChildTasks();

    public int getId() {
        return mTaskRecord.getId();
    }

    public abstract String getScheduleText(Context context);
}
