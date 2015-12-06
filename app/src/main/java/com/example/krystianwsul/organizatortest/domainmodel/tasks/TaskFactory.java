package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleScheduleRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class TaskFactory {
    private static TaskFactory sInstance;

    private final HashMap<Integer, RootTask> mRootTasks = new HashMap<>();
    private final HashMap<Integer, Task> mTasks = new HashMap<>();

    public static TaskFactory getInstance() {
        if (sInstance == null)
            sInstance = new TaskFactory();
        return sInstance;
    }

    private TaskFactory() {
        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        ArrayList<TaskRecord> parentTaskRecords = persistenceManger.getTaskRecords(null);
        Assert.assertTrue(parentTaskRecords != null);
        Assert.assertTrue(!parentTaskRecords.isEmpty());

        for (TaskRecord parentTaskRecord : parentTaskRecords) {
            Assert.assertTrue(parentTaskRecord != null);

            RootTask rootTask = new RootTask(parentTaskRecord);

            Schedule schedule = loadSchedule(rootTask);
            Assert.assertTrue(schedule != null);

            rootTask.setSchedule(schedule);

            initializeChildren(rootTask);

            mRootTasks.put(rootTask.getId(), rootTask);
            mTasks.put(rootTask.getId(), rootTask);
        }
    }

    private Schedule loadSchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        SingleScheduleRecord singleScheduleRecord = PersistenceManger.getInstance().getSingleScheduleRecord(rootTask.getId());
        if (singleScheduleRecord != null)
            return loadSingleSchedule(singleScheduleRecord, rootTask);

        DailyScheduleRecord dailyScheduleRecord = persistenceManger.getDailyScheduleRecord(rootTask.getId());
        if (dailyScheduleRecord != null)
            return loadDailySchedule(dailyScheduleRecord, rootTask);

        WeeklyScheduleRecord weeklyScheduleRecord = persistenceManger.getWeeklyScheduleRecord(rootTask.getId());
        if (weeklyScheduleRecord != null)
            return loadWeeklySchedule(weeklyScheduleRecord, rootTask);

        throw new IllegalArgumentException("no schedule for rootTask == " + rootTask);
    }

    private SingleSchedule loadSingleSchedule(SingleScheduleRecord singleScheduleRecord, RootTask rootTask) {
        Assert.assertTrue(singleScheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        SingleSchedule singleSchedule = new SingleSchedule(singleScheduleRecord, rootTask);
        SingleRepetitionFactory.getInstance().loadExistingSingleRepetition(singleSchedule);

        return singleSchedule;
    }

    private DailySchedule loadDailySchedule(DailyScheduleRecord dailyScheduleRecord, RootTask rootTask) {
        Assert.assertTrue(dailyScheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        DailySchedule dailySchedule = new DailySchedule(dailyScheduleRecord, rootTask);

        ArrayList<DailyScheduleTimeRecord> dailyScheduleTimeRecords = persistenceManger.getDailyScheduleTimeRecords(rootTask.getId());
        Assert.assertTrue(!dailyScheduleTimeRecords.isEmpty());

        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : dailyScheduleTimeRecords) {
            DailyScheduleTime dailyScheduleTime = new DailyScheduleTime(dailyScheduleTimeRecord, dailySchedule);
            DailyRepetitionFactory.getInstance().loadExistingDailyRepetitions(dailyScheduleTime);

            dailySchedule.addDailyScheduleTime(dailyScheduleTime);
        }

        return dailySchedule;
    }

    private WeeklySchedule loadWeeklySchedule(WeeklyScheduleRecord weeklyScheduleRecord, RootTask rootTask) {
        Assert.assertTrue(weeklyScheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        WeeklySchedule weeklySchedule = new WeeklySchedule(weeklyScheduleRecord, rootTask);

        ArrayList<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = persistenceManger.getWeeklyScheduleDayOfWeekTimeRecords(rootTask.getId());
        Assert.assertTrue(!weeklyScheduleDayOfWeekTimeRecords.isEmpty());

        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : weeklyScheduleDayOfWeekTimeRecords) {
            WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime = new WeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeRecord, weeklySchedule);
            WeeklyRepetitionFactory.getInstance().loadExistingWeeklyRepetitions(weeklyScheduleDayOfWeekTime);

            weeklySchedule.addWeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTime);
        }

        return weeklySchedule;
    }

    private void initializeChildren(Task task) {
        ArrayList<TaskRecord> childTaskRecords = PersistenceManger.getInstance().getTaskRecords(task.getId());
        Assert.assertTrue(childTaskRecords != null);

        for (TaskRecord childTaskRecord : childTaskRecords)
            task.addChildTask(loadChildTask(task, childTaskRecord));
    }

    private ChildTask loadChildTask(Task parentTask, TaskRecord childTaskRecord) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(childTaskRecord != null);

        ChildTask childTask = new ChildTask(childTaskRecord, parentTask);

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

    public RootTask createSingleScheduleTask(String name, Date date, Time time) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue(time != null);

        RootTask rootTask = createRootTask(name);

        SingleSchedule singleSchedule = createSingleSchedule(rootTask, date, time);
        Assert.assertTrue(singleSchedule != null);

        rootTask.setSchedule(singleSchedule);

        return rootTask;
    }

    public RootTask createDailyScheduleTask(String name, ArrayList<Time> times) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(times != null);
        Assert.assertTrue(!times.isEmpty());

        RootTask rootTask = createRootTask(name);

        DailySchedule dailySchedule = createDailySchedule(rootTask, times);
        Assert.assertTrue(dailySchedule != null);

        rootTask.setSchedule(dailySchedule);

        return rootTask;
    }

    public RootTask createWeeklyScheduleTask(String name, ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimePairs) {
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        RootTask rootTask = createRootTask(name);

        WeeklySchedule weeklySchedule = createWeeklySchedule(rootTask, dayOfWeekTimePairs);
        Assert.assertTrue(weeklySchedule != null);

        rootTask.setSchedule(weeklySchedule);

        return rootTask;
    }

    private RootTask createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        TaskRecord taskRecord = PersistenceManger.getInstance().createTaskRecord(null, name);
        Assert.assertTrue(taskRecord != null);

        RootTask rootTask = new RootTask(taskRecord);

        mRootTasks.put(rootTask.getId(), rootTask);
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    public Pair<ChildTask, Task> addChildTask(Task oldParentTask, String name) {
        Assert.assertTrue(oldParentTask != null);
        Assert.assertTrue(!TextUtils.isEmpty(name));

        RootTask oldRootTask = oldParentTask.getRootTask();
        Assert.assertTrue(oldRootTask != null);

        Pair<RootTask, Task> newPair = copyRootTask(oldRootTask, oldParentTask);
        RootTask newRootTask = newPair.first;
        Task newParentTask = newPair.second;
        Assert.assertTrue(newRootTask != null);
        Assert.assertTrue(newParentTask != null);

        ChildTask newChildTask = createChildTask(newParentTask, name);
        Assert.assertTrue(newChildTask != null);

        newParentTask.addChildTask(newChildTask);

        return new Pair<>(newChildTask, newParentTask);
    }

    private Pair<RootTask, Task> copyRootTask(RootTask oldRootTask, Task oldHoldTask) {
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
            Pair<ChildTask, Task> newPair = copyChildTask(newRootTask, oldChildTask, oldHoldTask);
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

        newRootTask.setSchedule(oldRootTask.mSchedule.copy(newRootTask));

        return new Pair<>(newRootTask, newHoldTask);
    }

    private Pair<ChildTask, Task> copyChildTask(Task newParentTask, ChildTask oldChildTask, Task oldHoldTask) {
        Assert.assertTrue(newParentTask != null);
        Assert.assertTrue(oldChildTask != null);
        Assert.assertTrue(oldHoldTask != null);

        ChildTask newChildTask = createChildTask(newParentTask, oldChildTask.getName());
        Assert.assertTrue(newChildTask != null);

        Task newHoldTask = null;
        if (oldChildTask == oldHoldTask)
            newHoldTask = newChildTask;

        for (ChildTask oldChildChildTask : oldChildTask.getChildTasks()) {
            Pair<ChildTask, Task> newPair = copyChildTask(newChildTask, oldChildChildTask, oldHoldTask);
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

    private ChildTask createChildTask(Task parentTask, String name) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(!TextUtils.isEmpty(name));

        TaskRecord childTaskRecord = PersistenceManger.getInstance().createTaskRecord(parentTask, name);
        Assert.assertTrue(childTaskRecord != null);

        ChildTask childTask = new ChildTask(childTaskRecord, parentTask);
        mTasks.put(childTask.getId(), childTask);
        return childTask;
    }

    private SingleSchedule createSingleSchedule(RootTask rootTask, Date date, Time time) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(date != null);
        Assert.assertTrue(time != null);

        SingleScheduleRecord singleScheduleRecord = PersistenceManger.getInstance().createSingleScheduleRecord(rootTask.getId(), date, time);
        Assert.assertTrue(singleScheduleRecord != null);

        return new SingleSchedule(singleScheduleRecord, rootTask);
    }

    DailySchedule copyDailySchedule(DailySchedule oldDailySchedule, RootTask newRootTask) {
        Assert.assertTrue(oldDailySchedule != null);
        Assert.assertTrue(newRootTask != null);

        oldDailySchedule.setEndTimeStamp();

        ArrayList<Time> times = new ArrayList<>();
        Assert.assertTrue(!oldDailySchedule.getDailyScheduleTimes().isEmpty());
        for (DailyScheduleTime dailyScheduleTime : oldDailySchedule.getDailyScheduleTimes()) {
            Assert.assertTrue(dailyScheduleTime != null);
            times.add(dailyScheduleTime.getTime());
        }

        return createDailySchedule(newRootTask, times);
    }

    private DailySchedule createDailySchedule(RootTask rootTask, ArrayList<Time> times) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(times != null);
        Assert.assertTrue(!times.isEmpty());

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        DailyScheduleRecord dailyScheduleRecord = persistenceManger.createDailyScheduleRecord(rootTask.getId());
        Assert.assertTrue(dailyScheduleRecord != null);

        DailySchedule dailySchedule = new DailySchedule(dailyScheduleRecord, rootTask);

        for (Time time : times) {
            Assert.assertTrue(time != null);

            DailyScheduleTimeRecord dailyScheduleTimeRecord = persistenceManger.createDailyScheduleTimeRecord(dailySchedule, time);
            Assert.assertTrue(dailyScheduleTimeRecord != null);

            dailySchedule.addDailyScheduleTime(new DailyScheduleTime(dailyScheduleTimeRecord, dailySchedule));
        }

        return dailySchedule;
    }

    WeeklySchedule copyWeeklySchedule(WeeklySchedule oldWeeklySchedule, RootTask newRootTask) {
        Assert.assertTrue(oldWeeklySchedule != null);
        Assert.assertTrue(newRootTask != null);

        oldWeeklySchedule.setEndTimeStamp();

        ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimePairs = new ArrayList<>();
        Assert.assertTrue(!oldWeeklySchedule.getWeeklyScheduleDayOfWeekTimes().isEmpty());
        for (WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime : oldWeeklySchedule.getWeeklyScheduleDayOfWeekTimes()) {
            Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);
            dayOfWeekTimePairs.add(new Pair<>(weeklyScheduleDayOfWeekTime.getDayOfWeek(), weeklyScheduleDayOfWeekTime.getTime()));
        }

        return createWeeklySchedule(newRootTask, dayOfWeekTimePairs);
    }

    private WeeklySchedule createWeeklySchedule(RootTask rootTask, ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimePairs) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        WeeklyScheduleRecord weeklyScheduleRecord = persistenceManger.createWeeklyScheduleRecord(rootTask.getId());
        Assert.assertTrue(weeklyScheduleRecord != null);

        WeeklySchedule weeklySchedule = new WeeklySchedule(weeklyScheduleRecord, rootTask);

        for (Pair<DayOfWeek, Time> dayOfWeekTimePair : dayOfWeekTimePairs) {
            Assert.assertTrue(dayOfWeekTimePair != null);

            DayOfWeek dayOfWeek = dayOfWeekTimePair.first;
            Time time = dayOfWeekTimePair.second;

            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(time != null);

            WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = persistenceManger.createWeeklyScheduleDayOfWeekTimeRecord(weeklySchedule, dayOfWeek, time);
            Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecord != null);

            weeklySchedule.addWeeklyScheduleDayOfWeekTime(new WeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeRecord, weeklySchedule));
        }

        return weeklySchedule;
    }
}
