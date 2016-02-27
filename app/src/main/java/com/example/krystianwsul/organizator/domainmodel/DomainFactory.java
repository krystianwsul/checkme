package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.loaders.CreateChildTaskLoader;
import com.example.krystianwsul.organizator.loaders.CreateRootTaskLoader;
import com.example.krystianwsul.organizator.loaders.DailyScheduleLoader;
import com.example.krystianwsul.organizator.loaders.EditInstanceLoader;
import com.example.krystianwsul.organizator.loaders.GroupListLoader;
import com.example.krystianwsul.organizator.loaders.ShowCustomTimeLoader;
import com.example.krystianwsul.organizator.loaders.ShowCustomTimesLoader;
import com.example.krystianwsul.organizator.loaders.ShowGroupLoader;
import com.example.krystianwsul.organizator.loaders.ShowInstanceLoader;
import com.example.krystianwsul.organizator.loaders.ShowNotificationGroupLoader;
import com.example.krystianwsul.organizator.loaders.ShowTaskLoader;
import com.example.krystianwsul.organizator.loaders.SingleScheduleLoader;
import com.example.krystianwsul.organizator.loaders.TaskListLoader;
import com.example.krystianwsul.organizator.loaders.WeeklyScheduleLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.persistencemodel.CustomTimeRecord;
import com.example.krystianwsul.organizator.persistencemodel.DailyScheduleTimeRecord;
import com.example.krystianwsul.organizator.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizator.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;
import com.example.krystianwsul.organizator.persistencemodel.SingleScheduleDateTimeRecord;
import com.example.krystianwsul.organizator.persistencemodel.TaskHierarchyRecord;
import com.example.krystianwsul.organizator.persistencemodel.TaskRecord;
import com.example.krystianwsul.organizator.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.ScheduleType;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class DomainFactory {
    private final PersistenceManger mPersistenceManager;

    private final HashMap<Integer, CustomTime> mCustomTimes = new HashMap<>();
    private final HashMap<Integer, Task> mTasks = new HashMap<>();
    private final HashMap<Integer, TaskHierarchy> mTaskHierarchies = new HashMap<>();
    private final ArrayList<Instance> mExistingInstances = new ArrayList<>();

    private static ArrayList<Observer> sObservers = new ArrayList<>();

    public static DomainFactory getDomainFactory(Context context) {
        Assert.assertTrue(context != null);

        DomainFactory domainFactory = new DomainFactory(context);
        domainFactory.initialize();
        return domainFactory;
    }

    private DomainFactory(Context context) {
        mPersistenceManager = new PersistenceManger(context);
    }

    private void initialize() {
        Collection<CustomTimeRecord> customTimeRecords = mPersistenceManager.getCustomTimeRecords();
        Assert.assertTrue(customTimeRecords != null);

        for (CustomTimeRecord customTimeRecord : customTimeRecords) {
            Assert.assertTrue(customTimeRecord != null);

            CustomTime customTime = new CustomTime(customTimeRecord);
            mCustomTimes.put(customTime.getId(), customTime);
        }

        Collection<TaskRecord> taskRecords = mPersistenceManager.getTaskRecords();
        Assert.assertTrue(taskRecords != null);

        for (TaskRecord taskRecord : taskRecords) {
            Assert.assertTrue(taskRecord != null);

            Task task = new Task(this, taskRecord);

            ArrayList<Schedule> schedules = loadSchedules(task);
            Assert.assertTrue(schedules != null);

            task.addSchedules(schedules);

            Assert.assertTrue(!mTasks.containsKey(task.getId()));
            mTasks.put(task.getId(), task);
        }

        Collection<TaskHierarchyRecord> taskHierarchyRecords = mPersistenceManager.getTaskHierarchyRecords();
        Assert.assertTrue(taskHierarchyRecords != null);

        for (TaskHierarchyRecord taskHierarchyRecord : taskHierarchyRecords) {
            Assert.assertTrue(taskHierarchyRecord != null);

            Task parentTask = mTasks.get(taskHierarchyRecord.getParentTaskId());
            Assert.assertTrue(parentTask != null);

            Task childTask = mTasks.get(taskHierarchyRecord.getChildTaskId());
            Assert.assertTrue(childTask != null);

            TaskHierarchy taskHierarchy = new TaskHierarchy(taskHierarchyRecord, parentTask, childTask);

            Assert.assertTrue(!mTaskHierarchies.containsKey(taskHierarchy.getId()));
            mTaskHierarchies.put(taskHierarchy.getId(), taskHierarchy);
        }

        Collection<InstanceRecord> instanceRecords = mPersistenceManager.getInstanceRecords();
        Assert.assertTrue(instanceRecords != null);

        for (InstanceRecord instanceRecord : instanceRecords) {
            Task task = mTasks.get(instanceRecord.getTaskId());
            Assert.assertTrue(task != null);

            Instance instance = new Instance(this, task, instanceRecord);
            mExistingInstances.add(instance);
        }
    }

    public void save() {
        save(0);
    }

    private void save(int dataId) {
        mPersistenceManager.save();
        notifyDomainObservers(dataId);
    }

    public static synchronized void addDomainObserver(Observer observer) {
        Assert.assertTrue(observer != null);
        sObservers.add(observer);
    }

    public static synchronized void removeDomainObserver(Observer observer) {
        Assert.assertTrue(observer != null);
        Assert.assertTrue(sObservers.contains(observer));

        sObservers.remove(observer);
    }

    private synchronized void notifyDomainObservers(int dataId) {
        for (Observer observer : sObservers)
            observer.onDomainChanged(this, dataId);
    }

    public Instance getInstance(Task task, DateTime scheduleDateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDateTime != null);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Instance instance : mExistingInstances) {
            Assert.assertTrue(instance != null);
            if (instance.getTaskId() == task.getId() && instance.getScheduleDateTime().compareTo(scheduleDateTime) == 0)
                instances.add(instance);
        }

        if (!instances.isEmpty()) {
            Assert.assertTrue(instances.size() == 1);
            return instances.get(0);
        } else {
            return new Instance(this, task, scheduleDateTime);
        }
    }

    public EditInstanceLoader.Data getEditInstanceData(int taskId, Date date, int scheduleCustomTimeId) {
        Assert.assertTrue(date != null);

        CustomTime customTime = mCustomTimes.get(scheduleCustomTimeId);
        Assert.assertTrue(customTime != null);

        DateTime dateTime = new DateTime(date, customTime);

        return getEditInstanceData(taskId, dateTime);
    }

    public EditInstanceLoader.Data getEditInstanceData(int taskId, Date date, HourMinute scheduleHourMinute) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(scheduleHourMinute != null);

        DateTime dateTime = new DateTime(date, new NormalTime(scheduleHourMinute));

        return getEditInstanceData(taskId, dateTime);
    }

    private EditInstanceLoader.Data getEditInstanceData(int taskId, DateTime scheduleDateTime) {
        Assert.assertTrue(scheduleDateTime != null);

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        Instance instance = getInstance(task, scheduleDateTime);

        HashMap<Integer, EditInstanceLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : getCurrentCustomTimes())
            customTimeDatas.put(customTime.getId(), new EditInstanceLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstanceLoader.Data(instance.getTaskId(), instance.getScheduleDate(), instance.getScheduleCustomTimeId(), instance.getScheduleHourMinute(), instance.getInstanceDate(), instance.getInstanceCustomTimeId(), instance.getInstanceHourMinute(), instance.getName(), customTimeDatas);
    }

    private ArrayList<Instance> getRootInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        Assert.assertTrue(endTimeStamp != null);
        Assert.assertTrue(startTimeStamp == null || startTimeStamp.compareTo(endTimeStamp) < 0);

        HashSet<Instance> allInstances = new HashSet<>();
        allInstances.addAll(mExistingInstances);

        for (Task task : mTasks.values())
            allInstances.addAll(task.getInstances(startTimeStamp, endTimeStamp));

        ArrayList<Instance> rootInstances = new ArrayList<>();
        for (Instance instance : allInstances)
            if (instance.isRootInstance())
                rootInstances.add(instance);

        return rootInstances;
    }

    public ArrayList<Instance> getCurrentInstances() {
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.DATE, 2);
        Date endDate = new Date(endCalendar);

        return getRootInstances(null, new TimeStamp(endDate, new HourMinute(0, 0)));
    }

    public ArrayList<Instance> getNotificationInstances() {
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.MINUTE, 1);

        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ArrayList<Instance> rootInstances = getRootInstances(null, endTimeStamp);

        ArrayList<Instance> notificationInstances = new ArrayList<>();
        for (Instance instance : rootInstances) {
            if (instance.getDone() == null && !instance.getNotified() && instance.getInstanceDateTime().getTimeStamp().compareTo(endTimeStamp) < 0)
                notificationInstances.add(instance);
        }
        return notificationInstances;
    }

    public ArrayList<Instance> getCurrentInstances(TimeStamp timeStamp) {
        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ArrayList<Instance> rootInstances = getRootInstances(timeStamp, endTimeStamp);

        ArrayList<Instance> currentInstances = new ArrayList<>();
        for (Instance instance : rootInstances)
            if (instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                currentInstances.add(instance);

        return currentInstances;
    }

    public ArrayList<Instance> getShownInstances() {
        ArrayList<Instance> shownInstances = new ArrayList<>();

        for (Instance instance : mExistingInstances)
            if (instance.getNotificationShown())
                shownInstances.add(instance);

        return shownInstances;
    }

    InstanceRecord createInstanceRecord(Task task, Instance instance, DateTime scheduleDateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(instance != null);
        Assert.assertTrue(scheduleDateTime != null);

        mExistingInstances.add(instance);

        return mPersistenceManager.createInstanceRecord(task, scheduleDateTime);
    }

    private DateTime getDateTime(Date date, Integer customTimeId, HourMinute hourMinute) {
        Assert.assertTrue(date != null);

        Time time = getTime(customTimeId, hourMinute);
        Assert.assertTrue(time != null);

        return new DateTime(date, time);
    }

    private Time getTime(Integer customTimeId, HourMinute hourMinute) {
        if (customTimeId != null) {
            Assert.assertTrue(hourMinute == null);

            CustomTime customTime = mCustomTimes.get(customTimeId);
            Assert.assertTrue(customTime != null);

            return customTime;
        } else {
            Assert.assertTrue(hourMinute != null);
            return new NormalTime(hourMinute);
        }
    }

    public void setInstanceDateTime(int dataId, Context context, int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute, Date instanceDate, Integer instanceCustomTimeId, HourMinute instanceHourMinute) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(scheduleDate != null);
        Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));
        Assert.assertTrue(instanceDate != null);
        Assert.assertTrue((instanceCustomTimeId == null) != (instanceHourMinute == null));

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        DateTime scheduleDateTime = getDateTime(scheduleDate, scheduleCustomTimeId, scheduleHourMinute);
        Assert.assertTrue(scheduleDateTime != null);

        Instance instance = getInstance(task, scheduleDateTime);
        Assert.assertTrue(instance != null);

        DateTime instanceDateTime = getDateTime(instanceDate, instanceCustomTimeId, instanceHourMinute);
        instance.setInstanceDateTime(context, instanceDateTime);

        save(dataId);
    }

    public void setInstanceDone(Context context, Instance instance, boolean done) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instance != null);

        instance.setDone(done, context);
    }

    public TimeStamp setInstanceDone(int dataId, Context context, int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute, boolean done) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(scheduleDate != null);
        Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        DateTime scheduleDateTime = getDateTime(scheduleDate, scheduleCustomTimeId, scheduleHourMinute);
        Assert.assertTrue(scheduleDateTime != null);

        Instance instance = getInstance(task, scheduleDateTime);
        Assert.assertTrue(instance != null);

        setInstanceDone(context, instance, done);

        save(dataId);

        return instance.getDone();
    }

    public void setInstancesNotified(ArrayList<Instance> instances) {
        Assert.assertTrue(instances != null);
        Assert.assertTrue(!instances.isEmpty());

        for (Instance instance : instances) {
            Assert.assertTrue(instance != null);
            instance.setNotified();
        }
    }

    public void setInstanceKeysNotified(int dataId, ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        ArrayList<Instance> instances = new ArrayList<>();
        for (InstanceKey instanceKey : instanceKeys)
            instances.add(getInstance(instanceKey.TaskId, instanceKey.ScheduleDate, instanceKey.ScheduleCustomTimeId, instanceKey.ScheduleHourMinute));

        setInstancesNotified(instances);

        save(dataId);
    }

    public void setInstanceNotifiedNotShown(Instance instance) {
        Assert.assertTrue(instance != null);

        instance.setNotified();
        instance.setNotificationShown(false);
    }

    private Instance getInstance(InstanceKey instanceKey) {
        Task task = mTasks.get(instanceKey.TaskId);
        Assert.assertTrue(task != null);

        DateTime scheduleDateTime = getDateTime(instanceKey.ScheduleDate, instanceKey.ScheduleCustomTimeId, instanceKey.ScheduleHourMinute);
        Assert.assertTrue(scheduleDateTime != null);

        Instance instance = getInstance(task, scheduleDateTime);
        Assert.assertTrue(instance != null);

        return instance;
    }

    public void setInstanceNotifiedNotShown(int dataId, InstanceKey instanceKey) {
        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        setInstanceNotifiedNotShown(instance);

        save(dataId);
    }

    public void updateInstancesShown(int dataId, ArrayList<InstanceKey> showInstanceKeys, ArrayList<InstanceKey>hideInstanceKeys) {
        Assert.assertTrue(hideInstanceKeys != null);

        if (showInstanceKeys != null) {
            for (InstanceKey showInstanceKey : showInstanceKeys) {
                Assert.assertTrue(showInstanceKey != null);

                Instance showInstance = getInstance(showInstanceKey);
                Assert.assertTrue(showInstance != null);

                showInstance.setNotificationShown(true);
            }
        }

        for (InstanceKey hideInstanceKey : hideInstanceKeys) {
            Assert.assertTrue(hideInstanceKey != null);

            Instance hideInstance = getInstance(hideInstanceKey);
            Assert.assertTrue(hideInstance != null);

            hideInstance.setNotificationShown(false);
        }

        save(dataId);
    }

    public void setInstancesShown(ArrayList<Instance> instances) {
        Assert.assertTrue(instances != null);
        Assert.assertTrue(!instances.isEmpty());

        for (Instance instance : instances)
            instance.setNotificationShown(true);
    }

    private ArrayList<Schedule> loadSchedules(Task task) {
        Assert.assertTrue(task != null);

        ArrayList<ScheduleRecord> scheduleRecords = mPersistenceManager.getScheduleRecords(task);
        Assert.assertTrue(scheduleRecords != null);

        ArrayList<Schedule> schedules = new ArrayList<>();

        for (ScheduleRecord scheduleRecord : scheduleRecords) {
            Assert.assertTrue(scheduleRecord.getType() >= 0);
            Assert.assertTrue(scheduleRecord.getType() < ScheduleType.values().length);

            ScheduleType scheduleType = ScheduleType.values()[scheduleRecord.getType()];

            switch (scheduleType) {
                case SINGLE:
                    schedules.add(loadSingleSchedule(scheduleRecord, task));
                    break;
                case DAILY:
                    schedules.add(loadDailySchedule(scheduleRecord, task));
                    break;
                case WEEKLY:
                    schedules.add(loadWeeklySchedule(scheduleRecord, task));
                    break;
                default:
                    throw new IndexOutOfBoundsException("unknown schedule type");
            }
        }

        return schedules;
    }

    private Schedule loadSingleSchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        SingleSchedule singleSchedule = new SingleSchedule(scheduleRecord, rootTask);

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = mPersistenceManager.getSingleScheduleDateTimeRecord(singleSchedule);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        SingleScheduleDateTime singleScheduleDateTime = new SingleScheduleDateTime(this, singleScheduleDateTimeRecord);
        singleSchedule.setSingleScheduleDateTime(singleScheduleDateTime);

        return singleSchedule;
    }

    private Schedule loadDailySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        DailySchedule dailySchedule = new DailySchedule(scheduleRecord, rootTask);

        ArrayList<DailyScheduleTimeRecord> dailyScheduleTimeRecords = mPersistenceManager.getDailyScheduleTimeRecords(dailySchedule);
        Assert.assertTrue(dailyScheduleTimeRecords != null);
        Assert.assertTrue(!dailyScheduleTimeRecords.isEmpty());

        for (DailyScheduleTimeRecord dailyScheduleTimeRecord : dailyScheduleTimeRecords) {
            DailyScheduleTime dailyScheduleTime = new DailyScheduleTime(this, dailyScheduleTimeRecord);
            dailySchedule.addDailyScheduleTime(dailyScheduleTime);
        }

        return dailySchedule;
    }

    private Schedule loadWeeklySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        WeeklySchedule weeklySchedule = new WeeklySchedule(scheduleRecord, rootTask);

        ArrayList<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = mPersistenceManager.getWeeklyScheduleDayOfWeekTimeRecords(weeklySchedule);
        Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecords != null);
        Assert.assertTrue(!weeklyScheduleDayOfWeekTimeRecords.isEmpty());

        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : weeklyScheduleDayOfWeekTimeRecords) {
            WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime = new WeeklyScheduleDayOfWeekTime(this, weeklyScheduleDayOfWeekTimeRecord);
            weeklySchedule.addWeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTime);
        }

        return weeklySchedule;
    }

    public ArrayList<Task> getRootTasks(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);

        ArrayList<Task> rootTasks = new ArrayList<>();
        for (Task task : mTasks.values()) {
            if (task.current(timeStamp) && task.isRootTask(timeStamp))
                rootTasks.add(task);
        }

        return rootTasks;
    }

    public Task getTask(int taskId) {
        return mTasks.get(taskId);
    }

    private Task createRootTask(String name, TimeStamp startTimeStamp) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(startTimeStamp != null);

        TaskRecord taskRecord = mPersistenceManager.createTaskRecord(name, startTimeStamp);
        Assert.assertTrue(taskRecord != null);

        Task rootTask = new Task(this, taskRecord);

        Assert.assertTrue(!mTasks.containsKey(rootTask.getId()));
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    public void createSingleScheduleRootTask(int dataId, String name, Date date, Integer customTimeId, HourMinute hourMinute) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue((customTimeId == null) != (hourMinute == null));

        TimeStamp timeStamp = TimeStamp.getNow();

        Task rootTask = createRootTask(name, timeStamp);
        Assert.assertTrue(rootTask != null);

        Time time;
        if (customTimeId != null) {
            time = mCustomTimes.get(customTimeId);
            Assert.assertTrue(time != null);
        } else {
            time = new NormalTime(hourMinute);
        }

        Schedule schedule = createSingleSchedule(rootTask, date, time, timeStamp);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    private ArrayList<Time> pairsToTimes(ArrayList<Pair<Integer, HourMinute>> timePairs) {
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        ArrayList<Time> times = new ArrayList<>();
        for (Pair<Integer, HourMinute> pair : timePairs)
            times.add(pairToTime(pair));

        return times;
    }

    private Time pairToTime(Pair<Integer, HourMinute> pair) {
        Assert.assertTrue(pair != null);

        if (pair.first != null) {
            Assert.assertTrue(pair.second == null);

            CustomTime customTime = mCustomTimes.get(pair.first);
            Assert.assertTrue(customTime != null);

            return customTime;
        } else {
            Assert.assertTrue(pair.second != null);
            return new NormalTime(pair.second);
        }
    }

    public void createDailyScheduleRootTask(int dataId, String name, ArrayList<Pair<Integer, HourMinute>> timePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        TimeStamp timeStamp = TimeStamp.getNow();

        Task rootTask = createRootTask(name, timeStamp);
        Assert.assertTrue(rootTask != null);

        ArrayList<Time> times = pairsToTimes(timePairs);

        Schedule schedule = createDailySchedule(rootTask, times, timeStamp);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public void createWeeklyScheduleRootTask(int dataId, String name, ArrayList<Pair<DayOfWeek, Pair<Integer, HourMinute>>> dayOfWeekTimePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        TimeStamp timeStamp = TimeStamp.getNow();

        Task rootTask = createRootTask(name, timeStamp);
        Assert.assertTrue(rootTask != null);

        Schedule schedule = createWeeklySchedule(rootTask, dayOfWeekTimePairs, timeStamp);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public void updateSingleScheduleRootTask(int dataId, int rootTaskId, String name, Date date, Integer customTimeId, HourMinute hourMinute) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue((customTimeId == null) != (hourMinute == null));

        Task rootTask = mTasks.get(rootTaskId);
        Assert.assertTrue(rootTask != null);

        TimeStamp timeStamp = TimeStamp.getNow();
        Assert.assertTrue(rootTask.current(timeStamp));
        Assert.assertTrue(rootTask.isRootTask(timeStamp));

        rootTask.setName(name);
        rootTask.setScheduleEndTimeStamp(timeStamp);

        Schedule schedule = createSingleSchedule(rootTask, date, getTime(customTimeId, hourMinute), timeStamp);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public void updateDailyScheduleRootTask(int dataId, int rootTaskId, String name, ArrayList<Pair<Integer, HourMinute>> timePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        Task rootTask = mTasks.get(rootTaskId);
        Assert.assertTrue(rootTask != null);

        TimeStamp timeStamp = TimeStamp.getNow();
        Assert.assertTrue(rootTask.current(timeStamp));
        Assert.assertTrue(rootTask.isRootTask(timeStamp));

        rootTask.setName(name);
        rootTask.setScheduleEndTimeStamp(timeStamp);

        ArrayList<Time> times = pairsToTimes(timePairs);

        Schedule schedule = createDailySchedule(rootTask, times, timeStamp);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public void updateWeeklyScheduleRootTask(int dataId, int rootTaskId, String name, ArrayList<Pair<DayOfWeek, Pair<Integer, HourMinute>>> dayOfWeekTimePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        Task rootTask = mTasks.get(rootTaskId);
        Assert.assertTrue(rootTask != null);

        TimeStamp timeStamp = TimeStamp.getNow();
        Assert.assertTrue(rootTask.current(timeStamp));
        Assert.assertTrue(rootTask.isRootTask(timeStamp));

        rootTask.setName(name);
        rootTask.setScheduleEndTimeStamp(timeStamp);

        Schedule schedule = createWeeklySchedule(rootTask, dayOfWeekTimePairs, timeStamp);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public void createSingleScheduleJoinRootTask(int dataId, String name, Date date, Integer customTimeId, HourMinute hourMinute, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue((customTimeId == null) != (hourMinute == null));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        TimeStamp timeStamp = TimeStamp.getNow();

        Task rootTask = createRootTask(name, timeStamp);
        Assert.assertTrue(rootTask != null);

        Time time = getTime(customTimeId, hourMinute);
        Assert.assertTrue(time != null);

        Schedule schedule = createSingleSchedule(rootTask, date, time, timeStamp);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        ArrayList<Task> joinTasks = new ArrayList<>();
        for (int joinTaskId : joinTaskIds) {
            Task joinTask = mTasks.get(joinTaskId);
            Assert.assertTrue(joinTask != null);
            Assert.assertTrue(joinTask.isRootTask(timeStamp));
            Assert.assertTrue(joinTask.current(timeStamp));

            joinTasks.add(joinTask);
        }

        joinTasks(rootTask, joinTasks, timeStamp);

        save(dataId);
    }

    public void createDailyScheduleJoinRootTask(int dataId, String name, ArrayList<Pair<Integer, HourMinute>> timePairs, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        TimeStamp timeStamp = TimeStamp.getNow();

        Task rootTask = createRootTask(name, timeStamp);
        Assert.assertTrue(rootTask != null);

        ArrayList<Time> times = pairsToTimes(timePairs);

        Schedule schedule = createDailySchedule(rootTask, times, timeStamp);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        ArrayList<Task> joinTasks = new ArrayList<>();
        for (int joinTaskId : joinTaskIds) {
            Task joinTask = mTasks.get(joinTaskId);
            Assert.assertTrue(joinTask != null);
            Assert.assertTrue(joinTask.isRootTask(timeStamp));
            Assert.assertTrue(joinTask.current(timeStamp));
        }

        joinTasks(rootTask, joinTasks, timeStamp);

        save(dataId);
    }

    public void createWeeklyScheduleJoinRootTask(int dataId, String name, ArrayList<Pair<DayOfWeek, Pair<Integer, HourMinute>>> dayOfWeekTimePairs, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        TimeStamp timeStamp = TimeStamp.getNow();

        Task rootTask = createRootTask(name, timeStamp);
        Assert.assertTrue(rootTask != null);

        Schedule schedule = createWeeklySchedule(rootTask, dayOfWeekTimePairs, timeStamp);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        ArrayList<Task> joinTasks = new ArrayList<>();
        for (int joinTaskId : joinTaskIds) {
            Task joinTask = mTasks.get(joinTaskId);
            Assert.assertTrue(joinTask != null);
            Assert.assertTrue(joinTask.isRootTask(timeStamp));
            Assert.assertTrue(joinTask.current(timeStamp));

            joinTasks.add(joinTask);
        }

        joinTasks(rootTask, joinTasks, timeStamp);

        save(dataId);
    }

    public void createChildTask(Task parentTask, String name, TimeStamp startTimeStamp) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(parentTask.current(startTimeStamp));

        TaskRecord childTaskRecord = mPersistenceManager.createTaskRecord(name, startTimeStamp);
        Assert.assertTrue(childTaskRecord != null);

        Task childTask = new Task(this, childTaskRecord);
        Assert.assertTrue(!mTasks.containsKey(childTask.getId()));
        mTasks.put(childTask.getId(), childTask);

        createTaskHierarchy(parentTask, childTask, startTimeStamp);
    }

    public void createChildTask(int parentTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task parentTask = mTasks.get(parentTaskId);
        Assert.assertTrue(parentTask != null);

        createChildTask(parentTask, name, TimeStamp.getNow());

        save();
    }

    public void updateChildTask(Task childTask, String name) {
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(!TextUtils.isEmpty(name));

        childTask.setName(name);
    }

    public void updateChildTask(int dataId, int childTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task childTask = mTasks.get(childTaskId);
        Assert.assertTrue(childTask != null);

        updateChildTask(childTask, name);

        save(dataId);
    }

    private void joinTasks(Task rootTask, ArrayList<Task> childTasks, TimeStamp timeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(rootTask.current(timeStamp));
        Assert.assertTrue(rootTask.isRootTask(timeStamp));
        Assert.assertTrue(childTasks != null);
        Assert.assertTrue(childTasks.size() > 1);

        for (Task childTask : childTasks) {
            Assert.assertTrue(childTask != null);
            Assert.assertTrue(childTask.current(timeStamp));
            Assert.assertTrue(childTask.isRootTask(timeStamp));

            childTask.setScheduleEndTimeStamp(timeStamp);

            createTaskHierarchy(rootTask, childTask, timeStamp);
        }
    }

    private void createTaskHierarchy(Task parentTask, Task childTask, TimeStamp startTimeStamp) {
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(startTimeStamp));
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(childTask.current(startTimeStamp));

        TaskHierarchyRecord taskHierarchyRecord = mPersistenceManager.createTaskHierarchyRecord(parentTask, childTask, startTimeStamp);
        Assert.assertTrue(taskHierarchyRecord != null);

        TaskHierarchy taskHierarchy = new TaskHierarchy(taskHierarchyRecord, parentTask, childTask);
        Assert.assertTrue(!mTaskHierarchies.containsKey(taskHierarchy.getId()));
        mTaskHierarchies.put(taskHierarchy.getId(), taskHierarchy);
    }

    private SingleSchedule createSingleSchedule(Task rootTask, Date date, Time time, TimeStamp startTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(date != null);
        Assert.assertTrue(time != null);
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(new DateTime(date, time).getTimeStamp().compareTo(startTimeStamp) >= 0);

        ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.SINGLE, startTimeStamp);
        Assert.assertTrue(scheduleRecord != null);

        SingleSchedule singleSchedule = new SingleSchedule(scheduleRecord, rootTask);

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = mPersistenceManager.createSingleScheduleDateTimeRecord(singleSchedule, date, time);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        singleSchedule.setSingleScheduleDateTime(new SingleScheduleDateTime(this, singleScheduleDateTimeRecord));

        return singleSchedule;
    }

    private DailySchedule createDailySchedule(Task rootTask, ArrayList<Time> times, TimeStamp startTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(times != null);
        Assert.assertTrue(!times.isEmpty());
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(rootTask.current(startTimeStamp));

        ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.DAILY, startTimeStamp);
        Assert.assertTrue(scheduleRecord != null);

        DailySchedule dailySchedule = new DailySchedule(scheduleRecord, rootTask);

        for (Time time : times) {
            Assert.assertTrue(time != null);

            DailyScheduleTimeRecord dailyScheduleTimeRecord = mPersistenceManager.createDailyScheduleTimeRecord(dailySchedule, time);
            Assert.assertTrue(dailyScheduleTimeRecord != null);

            dailySchedule.addDailyScheduleTime(new DailyScheduleTime(this, dailyScheduleTimeRecord));
        }

        return dailySchedule;
    }

    private WeeklySchedule createWeeklySchedule(Task rootTask, ArrayList<Pair<DayOfWeek, Pair<Integer, HourMinute>>> dayOfWeekTimePairs, TimeStamp startTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(rootTask.current(startTimeStamp));

        ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.WEEKLY, startTimeStamp);
        Assert.assertTrue(scheduleRecord != null);

        WeeklySchedule weeklySchedule = new WeeklySchedule(scheduleRecord, rootTask);

        for (Pair<DayOfWeek, Pair<Integer, HourMinute>> dayOfWeekTimePair : dayOfWeekTimePairs) {
            Assert.assertTrue(dayOfWeekTimePair != null);

            DayOfWeek dayOfWeek = dayOfWeekTimePair.first;
            Time time = pairToTime(dayOfWeekTimePair.second);

            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(time != null);

            WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = mPersistenceManager.createWeeklyScheduleDayOfWeekTimeRecord(weeklySchedule, dayOfWeek, time);
            Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecord != null);

            weeklySchedule.addWeeklyScheduleDayOfWeekTime(new WeeklyScheduleDayOfWeekTime(this, weeklyScheduleDayOfWeekTimeRecord));
        }

        return weeklySchedule;
    }

    ArrayList<Task> getChildTasks(Task parentTask, TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(timeStamp));

        ArrayList<Task> childTasks = new ArrayList<>();
        for (TaskHierarchy taskHierarchy : mTaskHierarchies.values())
            if (taskHierarchy.current(timeStamp) && taskHierarchy.getChildTask().current(timeStamp) && taskHierarchy.getParentTask() == parentTask)
                childTasks.add(taskHierarchy.getChildTask());
        return childTasks;
    }

    Task getParentTask(Task childTask, TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(childTask.current(timeStamp));

        TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, timeStamp);
        if (parentTaskHierarchy == null) {
            return null;
        } else {
            Assert.assertTrue(parentTaskHierarchy.current(timeStamp));
            Task parentTask = parentTaskHierarchy.getParentTask();
            Assert.assertTrue(parentTask.current(timeStamp));
            return parentTask;
        }
    }

    private TaskHierarchy getParentTaskHierarchy(Task childTask, TimeStamp timeStamp) {
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(timeStamp != null);
        Assert.assertTrue(childTask.current(timeStamp));

        ArrayList<TaskHierarchy> taskHierarchies = new ArrayList<>();
        for (TaskHierarchy taskHierarchy : mTaskHierarchies.values()) {
            Assert.assertTrue(taskHierarchy != null);

            if (!taskHierarchy.current(timeStamp))
                continue;

            if (taskHierarchy.getChildTask() != childTask)
                continue;

            taskHierarchies.add(taskHierarchy);
        }

        if (taskHierarchies.isEmpty()) {
            return null;
        } else {
            Assert.assertTrue(taskHierarchies.size() == 1);
            return taskHierarchies.get(0);
        }
    }

    void setParentHierarchyEndTimeStamp(Task childTask, TimeStamp endTimeStamp) {
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(endTimeStamp != null);
        Assert.assertTrue(childTask.current(endTimeStamp));

        TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, endTimeStamp);
        if (parentTaskHierarchy != null) {
            Assert.assertTrue(parentTaskHierarchy.current(endTimeStamp));
            parentTaskHierarchy.setEndTimeStamp(endTimeStamp);
        }
    }

    public void setTaskEndTimeStamp(int dataId, int taskId) {
        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        TimeStamp timeStamp = TimeStamp.getNow();
        Assert.assertTrue(task.current(timeStamp));

        task.setEndTimeStamp(timeStamp);

        save(dataId);
    }

    public CustomTime getCustomTime(int customTimeId) {
        Assert.assertTrue(mCustomTimes.containsKey(customTimeId));
        return mCustomTimes.get(customTimeId);
    }

    public ShowCustomTimeLoader.Data getShowCustomTimeData(int customTimeId) {
        CustomTime customTime = mCustomTimes.get(customTimeId);
        Assert.assertTrue(customTime != null);

        HashMap<DayOfWeek, HourMinute> hourMinutes = new HashMap<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            HourMinute hourMinute = customTime.getHourMinute(dayOfWeek);
            Assert.assertTrue(hourMinute != null);

            hourMinutes.put(dayOfWeek, hourMinute);
        }

        return new ShowCustomTimeLoader.Data(customTime.getId(), customTime.getName(), hourMinutes);
    }

    public CustomTime getCustomTime(DayOfWeek dayOfWeek, HourMinute hourMinute) {
        for (CustomTime customTime : getCurrentCustomTimes())
            if (customTime.getHourMinute(dayOfWeek).equals(hourMinute))
                return customTime;
        return null;
    }

    public ArrayList<CustomTime> getCurrentCustomTimes() {
        ArrayList<CustomTime> customTimes = new ArrayList<>();
        for (CustomTime customTime : mCustomTimes.values())
            if (customTime.getCurrent())
                customTimes.add(customTime);
        return customTimes;
    }

    public ShowCustomTimesLoader.Data getShowCustomTimesData() {
        ArrayList<CustomTime> currentCustomTimes = getCurrentCustomTimes();

        ArrayList<ShowCustomTimesLoader.Data.Entry> entries = new ArrayList<>();
        for (CustomTime customTime : currentCustomTimes) {
            Assert.assertTrue(customTime != null);
            entries.add(new ShowCustomTimesLoader.Data.Entry(customTime.getId(), customTime.getName()));
        }

        return new ShowCustomTimesLoader.Data(entries);
    }

    public void createCustomTime(String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(hourMinutes != null);

        Assert.assertTrue(hourMinutes.get(DayOfWeek.SUNDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.MONDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.TUESDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.WEDNESDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.THURSDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.FRIDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.SATURDAY) != null);

        CustomTimeRecord customTimeRecord = mPersistenceManager.createCustomTimeRecord(name, hourMinutes);
        Assert.assertTrue(customTimeRecord != null);

        CustomTime customTime = new CustomTime(customTimeRecord);
        mCustomTimes.put(customTime.getId(), customTime);

        save();
    }

    public void updateCustomTime(int dataId, int customTimeId, String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(hourMinutes != null);

        CustomTime customTime = mCustomTimes.get(customTimeId);
        Assert.assertTrue(customTime != null);

        customTime.setName(name);

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            HourMinute hourMinute = hourMinutes.get(dayOfWeek);
            Assert.assertTrue(hourMinute != null);

            if (hourMinute.compareTo(customTime.getHourMinute(dayOfWeek)) != 0)
                customTime.setHourMinute(dayOfWeek, hourMinute);
        }

        save(dataId);
    }

    public void setCustomTimeCurrent(int dataId, int customTimeId) {
        CustomTime customTime = mCustomTimes.get(customTimeId);
        Assert.assertTrue(customTime != null);

        customTime.setCurrent();

        save(dataId);
    }

    public GroupListLoader.Data getGroupListData(Context context) {
        ArrayList<GroupListLoader.InstanceData> instanceDatas = new ArrayList<>();
        for (Instance instance : getCurrentInstances())
            instanceDatas.add(new GroupListLoader.InstanceData(instance.getDone(), !instance.getChildInstances().isEmpty(), instance.getTaskId(), instance.getScheduleDate(), instance.getScheduleCustomTimeId(), instance.getScheduleHourMinute(), instance.getDisplayText(context), instance.getName(), instance.getInstanceDateTime().getTimeStamp()));

        ArrayList<GroupListLoader.CustomTimeData> customTimeDatas = new ArrayList<>();
        for (CustomTime customTime : getCurrentCustomTimes())
            customTimeDatas.add(new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()));

        return new GroupListLoader.Data(instanceDatas, customTimeDatas);
    }

    public ShowGroupLoader.Data getShowGroupData(Context context, TimeStamp timeStamp) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(timeStamp != null);

        ArrayList<Instance> instances = getCurrentInstances(timeStamp);
        Assert.assertTrue(!instances.isEmpty());

        DateTime dateTime = instances.get(0).getInstanceDateTime();

        DayOfWeek dayOfWeek = dateTime.getDate().getDayOfWeek();
        HourMinute hourMinute = dateTime.getTime().getHourMinute(dayOfWeek);
        Time time = getCustomTime(dayOfWeek, hourMinute);
        if (time == null)
            time = new NormalTime(hourMinute);

        String displayText = new DateTime(instances.get(0).getInstanceDate(), time).getDisplayText(context);

        ArrayList<ShowGroupLoader.InstanceData> instanceDatas = new ArrayList<>();
        for (Instance instance : instances)
            instanceDatas.add(new ShowGroupLoader.InstanceData(instance.getDone(), instance.getName(), !instance.getChildInstances().isEmpty(), instance.getTaskId(), instance.getScheduleDate(), instance.getScheduleCustomTimeId(), instance.getScheduleHourMinute(), null));

        return new ShowGroupLoader.Data(displayText, instanceDatas);
    }

    private Instance getInstance(int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute) {
        Assert.assertTrue(scheduleDate != null);
        Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        DateTime scheduleDateTime = getDateTime(scheduleDate, scheduleCustomTimeId, scheduleHourMinute);
        Assert.assertTrue(scheduleDateTime != null);

        Instance instance = getInstance(task, scheduleDateTime);
        Assert.assertTrue(instance != null);

        return instance;
    }

    public ShowNotificationGroupLoader.Data getShowNotificationGroupData(Context context, ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        ArrayList<Instance> instances = new ArrayList<>();
        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey.TaskId, instanceKey.ScheduleDate, instanceKey.ScheduleCustomTimeId, instanceKey.ScheduleHourMinute);
            Assert.assertTrue(instance != null);

            instances.add(instance);
        }

        Collections.sort(instances, new Comparator<Instance>() {
            @Override
            public int compare(Instance lhs, Instance rhs) {
                return lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime());
            }
        });

        ArrayList<ShowNotificationGroupLoader.InstanceData> instanceDatas = new ArrayList<>();
        for (Instance instance : instances)
            instanceDatas.add(new ShowNotificationGroupLoader.InstanceData(instance.getDone(), instance.getName(), !instance.getChildInstances().isEmpty(), instance.getTaskId(), instance.getScheduleDate(), instance.getScheduleCustomTimeId(), instance.getScheduleHourMinute(), instance.getDisplayText(context)));

        return new ShowNotificationGroupLoader.Data(instanceDatas);
    }

    public ShowInstanceLoader.Data getShowInstanceData(Context context, int taskId, Date date, int scheduleCustomTimeId) {
        Assert.assertTrue(date != null);

        CustomTime customTime = mCustomTimes.get(scheduleCustomTimeId);
        Assert.assertTrue(customTime != null);

        DateTime dateTime = new DateTime(date, customTime);

        return getShowInstanceData(context, taskId, dateTime);
    }

    public ShowInstanceLoader.Data getShowInstanceData(Context context, int taskId, Date date, HourMinute scheduleHourMinute) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(scheduleHourMinute != null);

        DateTime dateTime = new DateTime(date, new NormalTime(scheduleHourMinute));

        return getShowInstanceData(context, taskId, dateTime);
    }

    private ShowInstanceLoader.Data getShowInstanceData(Context context, int taskId, DateTime scheduleDateTime) {
        Assert.assertTrue(scheduleDateTime != null);

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        Instance instance = getInstance(task, scheduleDateTime);

        ArrayList<ShowInstanceLoader.InstanceData> instanceDatas = new ArrayList<>();
        for (Instance childInstance : instance.getChildInstances())
            instanceDatas.add(new ShowInstanceLoader.InstanceData(childInstance.getDone(), childInstance.getName(), !childInstance.getChildInstances().isEmpty(), childInstance.getTaskId(), childInstance.getScheduleDate(), childInstance.getScheduleCustomTimeId(), childInstance.getScheduleHourMinute(), null));

        return new ShowInstanceLoader.Data(instance.getTaskId(), instance.getScheduleDate(), instance.getScheduleCustomTimeId(), instance.getScheduleHourMinute(), instance.getName(), instance.getDisplayText(context), instance.getDone() != null, !instance.getChildInstances().isEmpty(), instanceDatas);
    }

    public CreateChildTaskLoader.Data getCreateChildTaskData(int childTaskId) {
        Task childTask = mTasks.get(childTaskId);
        Assert.assertTrue(childTask != null);

        return new CreateChildTaskLoader.Data(childTask.getName());
    }

    public CreateRootTaskLoader.Data getCreateRootTaskData(int rootTaskId) {
        Task rootTask = mTasks.get(rootTaskId);
        Assert.assertTrue(rootTask != null);

        return new CreateRootTaskLoader.Data(rootTask.getName(), rootTask.getCurrentSchedule(TimeStamp.getNow()).getType());
    }

    public SingleScheduleLoader.Data getSingleScheduleData(Integer rootTaskId) {
        SingleScheduleLoader.ScheduleData scheduleData = null;

        if (rootTaskId != null) {
            Task rootTask = mTasks.get(rootTaskId);
            Assert.assertTrue(rootTask != null);

            SingleSchedule singleSchedule = (SingleSchedule) rootTask.getCurrentSchedule(TimeStamp.getNow());
            Assert.assertTrue(singleSchedule != null);
            Assert.assertTrue(singleSchedule.current(TimeStamp.getNow()));

            scheduleData = new SingleScheduleLoader.ScheduleData(singleSchedule.getDateTime().getDate(), singleSchedule.getCustomTimeId(), singleSchedule.getHourMinute());
        }

        ArrayList<CustomTime> customTimes = getCurrentCustomTimes();
        HashMap<Integer, SingleScheduleLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes)
            customTimeDatas.put(customTime.getId(), new SingleScheduleLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new SingleScheduleLoader.Data(scheduleData, customTimeDatas);
    }

    public DailyScheduleLoader.Data getDailyScheduleData(Integer rootTaskId) {
        ArrayList<DailyScheduleLoader.ScheduleData> scheduleDatas = null;

        if (rootTaskId != null) {
            Task rootTask = mTasks.get(rootTaskId);
            Assert.assertTrue(rootTask != null);

            DailySchedule dailySchedule = (DailySchedule) rootTask.getCurrentSchedule(TimeStamp.getNow());
            Assert.assertTrue(dailySchedule != null);
            Assert.assertTrue(dailySchedule.current(TimeStamp.getNow()));

            scheduleDatas = new ArrayList<>();
            for (Time time : dailySchedule.getTimes()) {
                if (time instanceof CustomTime)
                    scheduleDatas.add(new DailyScheduleLoader.ScheduleData(((CustomTime)time).getId(), null));
                else
                    scheduleDatas.add(new DailyScheduleLoader.ScheduleData(null, ((NormalTime)time).getHourMinute()));
            }
        }

        ArrayList<CustomTime> customTimes = getCurrentCustomTimes();
        HashMap<Integer, DailyScheduleLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes)
            customTimeDatas.put(customTime.getId(), new DailyScheduleLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new DailyScheduleLoader.Data(scheduleDatas, customTimeDatas);
    }

    public WeeklyScheduleLoader.Data getWeeklyScheduleData(Integer rootTaskId) {
        ArrayList<WeeklyScheduleLoader.ScheduleData> scheduleDatas = null;

        if (rootTaskId != null) {
            Task rootTask = mTasks.get(rootTaskId);
            Assert.assertTrue(rootTask != null);

            WeeklySchedule weeklySchedule = (WeeklySchedule) rootTask.getCurrentSchedule(TimeStamp.getNow());
            Assert.assertTrue(weeklySchedule != null);
            Assert.assertTrue(weeklySchedule.current(TimeStamp.getNow()));

            scheduleDatas = new ArrayList<>();
            for (Pair<DayOfWeek, Time> pair : weeklySchedule.getDayOfWeekTimes()) {
                if (pair.second instanceof CustomTime)
                    scheduleDatas.add(new WeeklyScheduleLoader.ScheduleData(pair.first, ((CustomTime)pair.second).getId(), null));
                else
                    scheduleDatas.add(new WeeklyScheduleLoader.ScheduleData(pair.first, null, ((NormalTime)pair.second).getHourMinute()));
            }
        }

        ArrayList<CustomTime> customTimes = getCurrentCustomTimes();
        HashMap<Integer, WeeklyScheduleLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes)
            customTimeDatas.put(customTime.getId(), new WeeklyScheduleLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new WeeklyScheduleLoader.Data(scheduleDatas, customTimeDatas);
    }

    public ShowTaskLoader.Data getShowTaskData(int taskId, Context context) {
        Assert.assertTrue(context != null);

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        TimeStamp timeStamp = TimeStamp.getNow();

        ArrayList<ShowTaskLoader.ChildTaskData> childTaskDatas = new ArrayList<>();
        for (Task childTask : task.getChildTasks(timeStamp))
            childTaskDatas.add(new ShowTaskLoader.ChildTaskData(childTask.getId(), childTask.getName(), !childTask.getChildTasks(timeStamp).isEmpty()));

        return new ShowTaskLoader.Data(task.isRootTask(timeStamp), task.getName(), task.getScheduleText(context, timeStamp), task.getId(), childTaskDatas);
    }

    public TaskListLoader.Data getTaskListData(Context context) {
        Assert.assertTrue(context != null);

        TimeStamp timeStamp = TimeStamp.getNow();

        ArrayList<TaskListLoader.RootTaskData> rootTaskDatas = new ArrayList<>();
        for (Task rootTask : getRootTasks(timeStamp))
            rootTaskDatas.add(new TaskListLoader.RootTaskData(rootTask.getId(), rootTask.getName(), rootTask.getScheduleText(context, timeStamp), !rootTask.getChildTasks(timeStamp).isEmpty()));

        return new TaskListLoader.Data(rootTaskDatas);
    }

    public TickService.Data getTickServiceData(Context context)
    {
        Assert.assertTrue(context != null);

        ArrayList<Instance> notificationInstances = getNotificationInstances();
        Assert.assertTrue(notificationInstances != null);

        ArrayList<TickService.NotificationInstanceData> notificationInstanceDatas = new ArrayList<>();
        for (Instance notificationInstance : notificationInstances)
            notificationInstanceDatas.add(new TickService.NotificationInstanceData(notificationInstance.getInstanceKey(), notificationInstance.getName(), notificationInstance.getNotificationId(), notificationInstance.getDisplayText(context)));

        ArrayList<Instance> shownInstances = getShownInstances();
        Assert.assertTrue(shownInstances != null);

        ArrayList<TickService.ShownInstanceData> shownInstanceDatas = new ArrayList<>();
        for (Instance shownInstance : shownInstances)
            shownInstanceDatas.add(new TickService.ShownInstanceData(shownInstance.getNotificationId(), shownInstance.getInstanceKey()));

        return new TickService.Data(notificationInstanceDatas, shownInstanceDatas);
    }

    public interface Observer {
        void onDomainChanged(DomainFactory domainFactory, int dataId);
    }
}
