package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.DailyScheduleFragment;
import com.example.krystianwsul.organizatortest.WeeklyScheduleFragment;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
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

            ArrayList<Schedule> schedules = ScheduleFactory.getInstance().getSchedules(rootTask);
            Assert.assertTrue(schedules != null);
            Assert.assertTrue(!schedules.isEmpty());
            rootTask.setSchedules(schedules);

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

        ArrayList<Schedule> schedules = new ArrayList<>();
        schedules.add(singleSchedule);

        rootTask.setSchedules(schedules);

        mRootTasks.put(rootTask.getId(), rootTask);
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    public RootTask createDailyScheduleTask(String name, ArrayList<DailyScheduleFragment.TimeEntry> timeEntries) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timeEntries != null);
        Assert.assertTrue(!timeEntries.isEmpty());

        TaskRecord taskRecord = PersistenceManger.getInstance().createTaskRecord(null, name);
        Assert.assertTrue(taskRecord != null);

        RootTask rootTask = new RootTask(taskRecord);

        DailySchedule dailySchedule = ScheduleFactory.getInstance().createDailySchedule(rootTask, timeEntries);
        Assert.assertTrue(dailySchedule != null);

        ArrayList<Schedule> schedules = new ArrayList<>();
        schedules.add(dailySchedule);

        rootTask.setSchedules(schedules);

        mRootTasks.put(rootTask.getId(), rootTask);
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    public RootTask createWeeklyScheduleTask(String name, ArrayList<WeeklyScheduleFragment.DayOfWeekTimeEntry> dayOfWeekTimeEntries) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimeEntries != null);
        Assert.assertTrue(!dayOfWeekTimeEntries.isEmpty());

        TaskRecord taskRecord = PersistenceManger.getInstance().createTaskRecord(null, name);
        Assert.assertTrue(taskRecord != null);

        RootTask rootTask = new RootTask(taskRecord);

        WeeklySchedule weeklySchedule = ScheduleFactory.getInstance().createWeeklySchedule(rootTask, dayOfWeekTimeEntries);
        Assert.assertTrue(weeklySchedule != null);

        ArrayList<Schedule> schedules = new ArrayList<>();
        schedules.add(weeklySchedule);

        rootTask.setSchedules(schedules);

        mRootTasks.put(rootTask.getId(), rootTask);
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    public ChildTask createChildTask(Task parentTask, String name) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(!TextUtils.isEmpty(name));

        TaskRecord taskRecord = PersistenceManger.getInstance().createTaskRecord(parentTask, name);
        Assert.assertTrue(taskRecord != null);

        ChildTask childTask = new ChildTask(taskRecord, parentTask);

        parentTask.addChildTask(childTask);

        mTasks.put(childTask.getId(), childTask);

        return childTask;
    }
}
