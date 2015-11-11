package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 10/13/2015.
 */
public abstract class Task {
    protected final TaskRecord mTaskRecord;
    protected ArrayList<ChildTask> mChildrenTasks = new ArrayList<>();

    private static final HashMap<Integer, Task> sTasks = new HashMap<>();

    protected Task(TaskRecord taskRecord) {
        Assert.assertTrue(taskRecord != null);
        mTaskRecord = taskRecord;
    }

    public String getName() {
        return mTaskRecord.getName();
    }

    public ArrayList<ChildTask> getChildTasks() {
        return mChildrenTasks;
    }

    public int getId() {
        return mTaskRecord.getId();
    }

    public abstract String getScheduleText(Context context);

    public void addChildTask(ChildTask childTask) {
        Assert.assertTrue(childTask != null);
        mChildrenTasks.add(childTask);
    }

    public abstract Schedule getSchedule();

    public abstract RootTask getRootTask();

    public abstract Task getParentTask();
}
