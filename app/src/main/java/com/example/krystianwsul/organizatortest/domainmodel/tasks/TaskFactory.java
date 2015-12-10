package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.ScheduleRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleScheduleDateTimeRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class TaskFactory {
    private static TaskFactory sInstance;

    private final ArrayList<RootTask> mRootTasks = new ArrayList<>();
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

            ArrayList<Schedule> schedules = loadSchedules(rootTask);
            Assert.assertTrue(schedules != null);
            Assert.assertTrue(!schedules.isEmpty());

            rootTask.addSchedules(schedules);

            initializeChildren(rootTask);

            mRootTasks.add(rootTask.getOrdinal(), rootTask);

            Assert.assertTrue(!mTasks.containsKey(rootTask.getId()));
            mTasks.put(rootTask.getId(), rootTask);
        }
    }

    private ArrayList<Schedule> loadSchedules(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        ArrayList<ScheduleRecord> scheduleRecords = PersistenceManger.getInstance().getScheduleRecords(rootTask);
        Assert.assertTrue(scheduleRecords != null);
        Assert.assertTrue(!scheduleRecords.isEmpty());

        ArrayList<Schedule> schedules = new ArrayList<>();

        for (ScheduleRecord scheduleRecord : scheduleRecords) {
            Assert.assertTrue(scheduleRecord.getType() >= 0);
            Assert.assertTrue(scheduleRecord.getType() < Schedule.ScheduleType.values().length);

            Schedule.ScheduleType scheduleType = Schedule.ScheduleType.values()[scheduleRecord.getType()];

            switch (scheduleType) {
                case SINGLE:
                    schedules.add(loadSingleSchedule(scheduleRecord, rootTask));
                    break;
                case DAILY:
                    schedules.add(loadDailySchedule(scheduleRecord, rootTask));
                    break;
                case WEEKLY:
                    schedules.add(loadWeeklySchedule(scheduleRecord, rootTask));
                    break;
                default:
                    throw new IndexOutOfBoundsException("unknown schedule type");
            }
        }

        Assert.assertTrue(!schedules.isEmpty());
        return schedules;
    }

    private Schedule loadSingleSchedule(ScheduleRecord scheduleRecord, RootTask rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        SingleSchedule singleSchedule = new SingleSchedule(scheduleRecord, rootTask);

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = PersistenceManger.getInstance().getSingleScheduleDateTimeRecord(singleSchedule);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        SingleScheduleDateTime singleScheduleDateTime = new SingleScheduleDateTime(singleScheduleDateTimeRecord, singleSchedule);
        SingleRepetitionFactory.getInstance().loadExistingSingleRepetition(singleScheduleDateTime);

        singleSchedule.addSingleScheduleDateTime(singleScheduleDateTime);

        return singleSchedule;
    }

    private Schedule loadDailySchedule(ScheduleRecord scheduleRecord, RootTask rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        DailySchedule dailySchedule = new DailySchedule(scheduleRecord, rootTask);

        ArrayList<DailyScheduleTimeRecord> dailyScheduleTimeRecords = persistenceManger.getDailyScheduleTimeRecords(dailySchedule);
        Assert.assertTrue(dailyScheduleTimeRecords != null);
        Assert.assertTrue(!dailyScheduleTimeRecords.isEmpty());

        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : dailyScheduleTimeRecords) {
            DailyScheduleTime dailyScheduleTime = new DailyScheduleTime(dailyScheduleTimeRecord, dailySchedule);
            DailyRepetitionFactory.getInstance().loadExistingDailyRepetitions(dailyScheduleTime);

            dailySchedule.addDailyScheduleTime(dailyScheduleTime);
        }

        return dailySchedule;
    }

    private Schedule loadWeeklySchedule(ScheduleRecord scheduleRecord, RootTask rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        WeeklySchedule weeklySchedule = new WeeklySchedule(scheduleRecord, rootTask);

        ArrayList<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = persistenceManger.getWeeklyScheduleDayOfWeekTimeRecords(weeklySchedule);
        Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecords != null);
        Assert.assertTrue(!weeklyScheduleDayOfWeekTimeRecords.isEmpty());

        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : weeklyScheduleDayOfWeekTimeRecords) {
            WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime = new WeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeRecord, weeklySchedule);
            WeeklyRepetitionFactory.getInstance().loadExistingWeeklyRepetitions(weeklyScheduleDayOfWeekTime);

            weeklySchedule.addWeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTime);
        }

        return weeklySchedule;
    }

    private void initializeChildren(Task task) {
        ArrayList<TaskRecord> childTaskRecords = PersistenceManger.getInstance().getTaskRecords(task);
        Assert.assertTrue(childTaskRecords != null);

        for (TaskRecord childTaskRecord : childTaskRecords)
            task.addChildTask(loadChildTask(task, childTaskRecord));
    }

    private ChildTask loadChildTask(Task parentTask, TaskRecord childTaskRecord) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(childTaskRecord != null);

        ChildTask childTask = new ChildTask(childTaskRecord, parentTask);

        initializeChildren(childTask);

        Assert.assertTrue(!mTasks.containsKey(childTask.getId()));
        mTasks.put(childTask.getId(), childTask);

        return childTask;
    }

    public Collection<RootTask> getRootTasks() {
        return mRootTasks;
    }

    public int getNextRootOrdinal() {
        if (mRootTasks.isEmpty()) {
            return 0;
        } else {
            ArrayList<Integer> ordinals = new ArrayList<>();
            for (RootTask rootTask : mRootTasks)
                ordinals.add(rootTask.getOrdinal());
            return Collections.max(ordinals) + 1;
        }
    }

    public Task getTask(int taskId) {
        return mTasks.get(taskId);
    }

    public RootTask createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        TaskRecord taskRecord = PersistenceManger.getInstance().createTaskRecord(null, name, getNextRootOrdinal());
        Assert.assertTrue(taskRecord != null);

        RootTask rootTask = new RootTask(taskRecord);

        mRootTasks.add(rootTask);

        Assert.assertTrue(!mTasks.containsKey(rootTask.getId()));
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    public void createChildTask(Task parentTask, String name) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(!TextUtils.isEmpty(name));

        TaskRecord childTaskRecord = PersistenceManger.getInstance().createTaskRecord(parentTask, name, parentTask.getNextChildOrdinal());
        Assert.assertTrue(childTaskRecord != null);

        ChildTask childTask = new ChildTask(childTaskRecord, parentTask);
        Assert.assertTrue(!mTasks.containsKey(childTask.getId()));
        mTasks.put(childTask.getId(), childTask);

        parentTask.addChildTask(childTask);
    }

    public SingleSchedule createSingleSchedule(RootTask rootTask, Date date, Time time) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(date != null);
        Assert.assertTrue(time != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        ScheduleRecord scheduleRecord = persistenceManger.createScheduleRecord(rootTask, Schedule.ScheduleType.SINGLE);
        Assert.assertTrue(scheduleRecord != null);

        SingleSchedule singleSchedule = new SingleSchedule(scheduleRecord, rootTask);

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = persistenceManger.createSingleScheduleDateTimeRecord(singleSchedule, date, time);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        singleSchedule.addSingleScheduleDateTime(new SingleScheduleDateTime(singleScheduleDateTimeRecord, singleSchedule));

        return singleSchedule;
    }

    public DailySchedule createDailySchedule(RootTask rootTask, ArrayList<Time> times) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(times != null);
        Assert.assertTrue(!times.isEmpty());

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        ScheduleRecord scheduleRecord = persistenceManger.createScheduleRecord(rootTask, Schedule.ScheduleType.DAILY);
        Assert.assertTrue(scheduleRecord != null);

        DailySchedule dailySchedule = new DailySchedule(scheduleRecord, rootTask);

        for (Time time : times) {
            Assert.assertTrue(time != null);

            DailyScheduleTimeRecord dailyScheduleTimeRecord = persistenceManger.createDailyScheduleTimeRecord(dailySchedule, time);
            Assert.assertTrue(dailyScheduleTimeRecord != null);

            dailySchedule.addDailyScheduleTime(new DailyScheduleTime(dailyScheduleTimeRecord, dailySchedule));
        }

        return dailySchedule;
    }

    public WeeklySchedule createWeeklySchedule(RootTask rootTask, ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimePairs) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        ScheduleRecord scheduleRecord = persistenceManger.createScheduleRecord(rootTask, Schedule.ScheduleType.WEEKLY);
        Assert.assertTrue(scheduleRecord != null);

        WeeklySchedule weeklySchedule = new WeeklySchedule(scheduleRecord, rootTask);

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
