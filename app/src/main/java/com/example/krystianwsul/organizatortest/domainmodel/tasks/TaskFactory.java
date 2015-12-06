package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailySchedule;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.ScheduleFactory;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklySchedule;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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

            Schedule schedule = ScheduleFactory.getInstance().getSchedule(rootTask);
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
            task.addChildTask(loadChildTask(task, childTaskId));
    }

    private ChildTask loadChildTask(Task parentTask, int childTaskId) {
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

    public RootTask createSingleScheduleTask(String name, Date date, CustomTime customTime, HourMinute hourMinute) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue((customTime == null) != (hourMinute == null));

        TaskRecord taskRecord = PersistenceManger.getInstance().createTaskRecord(null, name);
        Assert.assertTrue(taskRecord != null);

        RootTask rootTask = new RootTask(taskRecord);

        SingleSchedule singleSchedule = ScheduleFactory.getInstance().createSingleSchedule(rootTask, date, customTime, hourMinute);
        Assert.assertTrue(singleSchedule != null);

        rootTask.setSchedule(singleSchedule);

        mRootTasks.put(rootTask.getId(), rootTask);
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    public RootTask createDailyScheduleTask(String name, ArrayList<Pair<CustomTime, HourMinute>> timePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        TaskRecord taskRecord = PersistenceManger.getInstance().createTaskRecord(null, name);
        Assert.assertTrue(taskRecord != null);

        RootTask rootTask = new RootTask(taskRecord);

        DailySchedule dailySchedule = ScheduleFactory.getInstance().createDailySchedule(rootTask, timePairs);
        Assert.assertTrue(dailySchedule != null);

        rootTask.setSchedule(dailySchedule);

        mRootTasks.put(rootTask.getId(), rootTask);
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    public RootTask createWeeklyScheduleTask(String name, ArrayList<Pair<DayOfWeek, Pair<CustomTime, HourMinute>>> dayOfWeekTimePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        TaskRecord taskRecord = PersistenceManger.getInstance().createTaskRecord(null, name);
        Assert.assertTrue(taskRecord != null);

        RootTask rootTask = new RootTask(taskRecord);

        WeeklySchedule weeklySchedule = ScheduleFactory.getInstance().createWeeklySchedule(rootTask, dayOfWeekTimePairs);
        Assert.assertTrue(weeklySchedule != null);

        rootTask.setSchedule(weeklySchedule);

        mRootTasks.put(rootTask.getId(), rootTask);
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    public Pair<ChildTask, Task> addChildTask(Task oldParentTask, String name) {
        Assert.assertTrue(oldParentTask != null);
        Assert.assertTrue(!TextUtils.isEmpty(name));

        RootTask oldRootTask = oldParentTask.getRootTask();
        Assert.assertTrue(oldRootTask != null);

        Pair<RootTask, Task> newPair = copy(oldRootTask, oldParentTask);
        RootTask newRootTask = newPair.first;
        Task newParentTask = newPair.second;
        Assert.assertTrue(newRootTask != null);
        Assert.assertTrue(newParentTask != null);

        TaskRecord newChildTaskRecord = PersistenceManger.getInstance().createTaskRecord(oldParentTask, name);
        Assert.assertTrue(newChildTaskRecord != null);

        ChildTask newChildTask = new ChildTask(newChildTaskRecord, newParentTask);
        mTasks.put(newChildTask.getId(), newChildTask);

        newParentTask.addChildTask(newChildTask);

        return new Pair<>(newChildTask, newParentTask);
    }

    private Pair<RootTask, Task> copy(RootTask oldRootTask, Task oldHoldTask) {
        Assert.assertTrue(oldRootTask != null);
        Assert.assertTrue(oldHoldTask != null);

        if (oldRootTask.isMutable())
            return new Pair<>(oldRootTask, oldHoldTask);

        TaskRecord newRootTaskRecord = PersistenceManger.getInstance().createTaskRecord(null, oldRootTask.getName());
        Assert.assertTrue(newRootTaskRecord != null);

        RootTask newRootTask = new RootTask(newRootTaskRecord);
        mRootTasks.put(newRootTask.getId(), newRootTask);
        mTasks.put(newRootTask.getId(), newRootTask);

        Task newHoldTask = null;
        if (oldRootTask == oldHoldTask)
            newHoldTask = newRootTask;

        for (ChildTask oldChildTask : oldRootTask.getChildTasks()) {
            Pair<ChildTask, Task> newPair = copy(newRootTask, oldChildTask, oldHoldTask);
            ChildTask newChildTask = newPair.first;
            Assert.assertTrue(newChildTask != null);

            newRootTask.addChildTask(newChildTask);

            Task childHoldTask = newPair.second;
            if (childHoldTask != null) {
                Assert.assertTrue(newHoldTask == null);
                newHoldTask = childHoldTask;
            }
        }

        Assert.assertTrue(newHoldTask != null);

        newRootTask.setSchedule(ScheduleFactory.getInstance().copy(oldRootTask.mSchedule, newRootTask));

        return new Pair<>(newRootTask, newHoldTask);
    }

    private Pair<ChildTask, Task> copy(Task newParentTask, ChildTask oldChildTask, Task oldHoldTask) {
        Assert.assertTrue(newParentTask != null);
        Assert.assertTrue(oldChildTask != null);
        Assert.assertTrue(oldHoldTask != null);

        TaskRecord newChildTaskRecord = PersistenceManger.getInstance().createTaskRecord(newParentTask, oldChildTask.getName());
        Assert.assertTrue(newChildTaskRecord != null);

        ChildTask newChildTask = new ChildTask(newChildTaskRecord, newParentTask);mTasks.put(newChildTask.getId(), newChildTask);
        mTasks.put(newChildTask.getId(), newChildTask);

        Task newHoldTask = null;
        if (oldChildTask == oldHoldTask)
            newHoldTask = newChildTask;

        for (ChildTask oldChildChildTask : oldChildTask.getChildTasks()) {
            Pair<ChildTask, Task> newPair = copy(newChildTask, oldChildChildTask, oldHoldTask);
            ChildTask newChildChildTask = newPair.first;
            Assert.assertTrue(newChildChildTask != null);

            newChildTask.addChildTask(newChildChildTask);

            Task childHoldTask = newPair.second;
            if (childHoldTask != null) {
                Assert.assertTrue(newHoldTask == null);
                newHoldTask = childHoldTask;
            }
        }

        return new Pair<>(newChildTask, newHoldTask);
    }
}
