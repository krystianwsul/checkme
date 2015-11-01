package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.security.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 10/13/2015.
 */
public abstract class Task {
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

    public static ArrayList<Task> getTopTasks() {
        ArrayList<Integer> topTaskIds = PersistenceManger.getInstance().getTaskIds(null);
        Assert.assertTrue(!topTaskIds.isEmpty());

        ArrayList<Task> topTasks = new ArrayList<>();
        for (Integer topTaskId : topTaskIds)
            topTasks.add(getTask(topTaskId));
        return topTasks;
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

    public abstract ArrayList<Task> getChildTasks();

    public int getId() {
        return mTaskRecord.getId();
    }

    public abstract String getScheduleText(Context context);
}
