package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Krystian on 11/5/2015.
 */
public class TaskFactory {
    private static TaskFactory sInstance;

    private HashMap<Integer, RootTask> mRootTasks = new HashMap<>();
    private HashMap<Integer, Task> mTasks = new HashMap<>();

    public static TaskFactory getInstance() {
        if (sInstance == null)
            sInstance = new TaskFactory();
        return sInstance;
    }

    private TaskFactory() {
        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        ArrayList<Integer> parentTaskIds = persistenceManger.getTaskIds(null);

        for (Integer parentTaskId : parentTaskIds) {
            TaskRecord taskRecord = PersistenceManger.getInstance().getTaskRecord(parentTaskId);
            Assert.assertTrue(taskRecord != null);

            RootTask rootTask = new RootTask(taskRecord);

            Schedule schedule = Schedule.getSchedule(rootTask.getId());
            Assert.assertTrue(schedule != null);
            rootTask.setSchedule(schedule);

            initializeChildren(rootTask);

            mRootTasks.put(rootTask.getId(), rootTask);
            mTasks.put(rootTask.getId(), rootTask);
        }
    }

    private void initializeChildren(Task task) {
        ArrayList<Integer> childTaskIds = PersistenceManger.getInstance().getTaskIds(task.getId());
        for (Integer childTaskId : childTaskIds)
            task.addChildTask(createChildTask(task, childTaskId));
    }

    private ChildTask createChildTask(Task parentTask, int childTaskId) {
        PersistenceManger persistenceManger = PersistenceManger.getInstance();
        TaskRecord taskRecord = persistenceManger.getTaskRecord(childTaskId);

        ChildTask childTask = new ChildTask(taskRecord, parentTask);

        initializeChildren(childTask);

        mTasks.put(childTask.getId(), childTask);

        return childTask;
    }

    public Collection<RootTask> getRootTasks() {
        return mRootTasks.values();
    }

    public Task getTask(int taskId) {
        return mTasks.get(taskId);
    }
}
