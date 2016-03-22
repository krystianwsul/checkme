package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.gui.instances.InstanceListFragment;
import com.example.krystianwsul.organizator.loaders.CreateChildTaskLoader;
import com.example.krystianwsul.organizator.loaders.CreateRootTaskLoader;
import com.example.krystianwsul.organizator.loaders.DailyScheduleLoader;
import com.example.krystianwsul.organizator.loaders.DomainLoader;
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
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.HourMili;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimePair;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class DomainFactory {
    private static DomainFactory sDomainFactory;

    private final PersistenceManger mPersistenceManager;

    private final HashMap<Integer, CustomTime> mCustomTimes = new HashMap<>();
    private final HashMap<Integer, Task> mTasks = new HashMap<>();
    private final HashMap<Integer, TaskHierarchy> mTaskHierarchies = new HashMap<>();
    private final ArrayList<Instance> mExistingInstances = new ArrayList<>();

    private final ArrayList<WeakReference<DomainLoader.Observer>> mObservers = new ArrayList<>();

    private ExactTimeStamp mStart;
    private ExactTimeStamp mRead;
    private ExactTimeStamp mStop;

    public static synchronized DomainFactory getDomainFactory(Context context) {
        Assert.assertTrue(context != null);

        if (sDomainFactory == null) {
            sDomainFactory = new DomainFactory(context);
            sDomainFactory.initialize();
        }
        return sDomainFactory;
    }

    public synchronized void addDomainObserver(DomainLoader.Observer observer) {
        Assert.assertTrue(observer != null);
        mObservers.add(new WeakReference<>(observer));
    }

    public synchronized void reset() {
        sDomainFactory = null;
        mPersistenceManager.reset();

        notifyDomainObservers(0);

        mObservers.clear();
    }

    private DomainFactory(Context context) {
        mStart = ExactTimeStamp.getNow();

        mPersistenceManager = PersistenceManger.getInstance(context);
        Assert.assertTrue(mPersistenceManager != null);

        mRead = ExactTimeStamp.getNow();
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

        mStop = ExactTimeStamp.getNow();
    }

    public long getReadMillis() {
        return (mRead.getLong() - mStart.getLong());
    }

    public long getTotalMillis() {
        return (mStop.getLong() - mStart.getLong());
    }

    private void save(int dataId) {
        mPersistenceManager.save();
        notifyDomainObservers(dataId);
    }

    private void notifyDomainObservers(int dataId) {
        ArrayList<WeakReference<DomainLoader.Observer>> remove = new ArrayList<>();

        for (WeakReference<DomainLoader.Observer> reference : mObservers) {
            Assert.assertTrue(reference != null);

            DomainLoader.Observer observer = reference.get();
            if (observer == null)
                remove.add(reference);
            else
                observer.onDomainChanged(dataId);
        }

        for (WeakReference<DomainLoader.Observer> reference : remove)
            mObservers.remove(reference);
    }

    // gets

    public synchronized EditInstanceLoader.Data getEditInstanceData(InstanceKey instanceKey) {
        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ArrayList<CustomTime> currentCustomTimes = getCurrentCustomTimes();
        HashMap<Integer, EditInstanceLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : currentCustomTimes)
            customTimeDatas.put(customTime.getId(), new EditInstanceLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstanceLoader.Data(instance.getInstanceKey(), instance.getInstanceDate(), instance.getInstanceTimePair(), instance.getName(), customTimeDatas);
    }

    public synchronized ShowCustomTimeLoader.Data getShowCustomTimeData(int customTimeId) {
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

    public synchronized ShowCustomTimesLoader.Data getShowCustomTimesData() {
        ArrayList<CustomTime> currentCustomTimes = getCurrentCustomTimes();

        ArrayList<ShowCustomTimesLoader.CustomTimeData> entries = new ArrayList<>();
        for (CustomTime customTime : currentCustomTimes) {
            Assert.assertTrue(customTime != null);
            entries.add(new ShowCustomTimesLoader.CustomTimeData(customTime.getId(), customTime.getName()));
        }

        return new ShowCustomTimesLoader.Data(entries);
    }

    public synchronized GroupListLoader.Data getGroupListData(Context context) {
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.DATE, 2);
        Date endDate = new Date(endCalendar);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        ArrayList<Instance> currentInstances = getRootInstances(null, new ExactTimeStamp(endDate, new HourMili(0, 0, 0, 0)), now);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances)
            instanceDatas.put(instance.getInstanceKey(), new GroupListLoader.InstanceData(instance.getDone(), !instance.getChildInstances(now).isEmpty(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp()));

        ArrayList<CustomTime> currentCustomTimes = getCurrentCustomTimes();
        ArrayList<GroupListLoader.CustomTimeData> customTimeDatas = new ArrayList<>();
        for (CustomTime customTime : currentCustomTimes)
            customTimeDatas.add(new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()));

        return new GroupListLoader.Data(instanceDatas, customTimeDatas);
    }

    public synchronized ShowGroupLoader.Data getShowGroupData(Context context, TimeStamp timeStamp) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(timeStamp != null);

        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        ArrayList<Instance> rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now);

        ArrayList<Instance> currentInstances = new ArrayList<>();
        for (Instance instance : rootInstances)
            if (instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                currentInstances.add(instance);
        Assert.assertTrue(!currentInstances.isEmpty());

        Date date = timeStamp.getDate();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        HourMinute hourMinute = timeStamp.getHourMinute();

        ArrayList<CustomTime> currentCustomTimes = getCurrentCustomTimes();
        Time time = null;
        for (CustomTime customTime : currentCustomTimes)
            if (customTime.getHourMinute(dayOfWeek).equals(hourMinute))
                time = customTime;
        if (time == null)
            time = new NormalTime(hourMinute);

        String displayText = new DateTime(date, time).getDisplayText(context);

        HashMap<InstanceKey, InstanceListFragment.InstanceAdapter.Data> instanceAdapterDatas = new HashMap<>();
        for (Instance instance : currentInstances)
            instanceAdapterDatas.put(instance.getInstanceKey(), new InstanceListFragment.InstanceAdapter.Data(instance.getDone(), instance.getName(), !instance.getChildInstances(now).isEmpty(), instance.getInstanceKey(), null));

        return new ShowGroupLoader.Data(displayText, instanceAdapterDatas);
    }

    public synchronized ShowNotificationGroupLoader.Data getShowNotificationGroupData(Context context, ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        ArrayList<Instance> instances = new ArrayList<>();
        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);
            Assert.assertTrue(instance != null);

            instances.add(instance);
        }

        Collections.sort(instances, new Comparator<Instance>() {
            @Override
            public int compare(Instance lhs, Instance rhs) {
                return lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime());
            }
        });

        ArrayList<InstanceListFragment.InstanceAdapter.Data> instanceDatas = new ArrayList<>();
        for (Instance instance : instances)
            instanceDatas.add(new InstanceListFragment.InstanceAdapter.Data(instance.getDone(), instance.getName(), !instance.getChildInstances(now).isEmpty(), instance.getInstanceKey(), instance.getDisplayText(context, now)));

        return new ShowNotificationGroupLoader.Data(instanceDatas);
    }

    public synchronized ShowInstanceLoader.Data getShowInstanceData(Context context, InstanceKey instanceKey) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        HashMap<InstanceKey, InstanceListFragment.InstanceAdapter.Data> instanceAdapterDatas = new HashMap<>();

        ArrayList<Instance> childInstances = instance.getChildInstances(now);
        for (Instance childInstance : childInstances)
            instanceAdapterDatas.put(childInstance.getInstanceKey(), new InstanceListFragment.InstanceAdapter.Data(childInstance.getDone(), childInstance.getName(), !childInstance.getChildInstances(now).isEmpty(), childInstance.getInstanceKey(), null));

        return new ShowInstanceLoader.Data(instance.getInstanceKey(), instance.getName(), instance.getDisplayText(context, now), instance.getDone() != null, !instance.getChildInstances(now).isEmpty(), instanceAdapterDatas);
    }

    public synchronized CreateChildTaskLoader.Data getCreateChildTaskData(int childTaskId) {
        Task childTask = mTasks.get(childTaskId);
        Assert.assertTrue(childTask != null);

        return new CreateChildTaskLoader.Data(childTask.getName());
    }

    public synchronized CreateRootTaskLoader.Data getCreateRootTaskData(int rootTaskId) {
        Task rootTask = mTasks.get(rootTaskId);
        Assert.assertTrue(rootTask != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        return new CreateRootTaskLoader.Data(rootTask.getName(), rootTask.getCurrentSchedule(now).getType());
    }

    public synchronized SingleScheduleLoader.Data getSingleScheduleData(Integer rootTaskId) {
        SingleScheduleLoader.ScheduleData scheduleData = null;

        if (rootTaskId != null) {
            Task rootTask = mTasks.get(rootTaskId);
            Assert.assertTrue(rootTask != null);

            ExactTimeStamp now = ExactTimeStamp.getNow();

            SingleSchedule singleSchedule = (SingleSchedule) rootTask.getCurrentSchedule(now);
            Assert.assertTrue(singleSchedule != null);
            Assert.assertTrue(singleSchedule.current(now));

            Instance instance = singleSchedule.getInstance(rootTask);

            scheduleData = new SingleScheduleLoader.ScheduleData(instance.getInstanceDate(), instance.getInstanceTimePair());
        }

        ArrayList<CustomTime> customTimes = getCurrentCustomTimes();
        HashMap<Integer, SingleScheduleLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes)
            customTimeDatas.put(customTime.getId(), new SingleScheduleLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new SingleScheduleLoader.Data(scheduleData, customTimeDatas);
    }

    public synchronized DailyScheduleLoader.Data getDailyScheduleData(Integer rootTaskId) {
        ArrayList<DailyScheduleLoader.ScheduleData> scheduleDatas = null;

        if (rootTaskId != null) {
            Task rootTask = mTasks.get(rootTaskId);
            Assert.assertTrue(rootTask != null);

            ExactTimeStamp now = ExactTimeStamp.getNow();

            DailySchedule dailySchedule = (DailySchedule) rootTask.getCurrentSchedule(now);
            Assert.assertTrue(dailySchedule != null);
            Assert.assertTrue(dailySchedule.current(now));

            ArrayList<Time> times = dailySchedule.getTimes();
            scheduleDatas = new ArrayList<>();
            for (Time time : times)
                scheduleDatas.add(new DailyScheduleLoader.ScheduleData(time.getTimePair()));
        }

        ArrayList<CustomTime> customTimes = getCurrentCustomTimes();
        HashMap<Integer, DailyScheduleLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes)
            customTimeDatas.put(customTime.getId(), new DailyScheduleLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new DailyScheduleLoader.Data(scheduleDatas, customTimeDatas);
    }

    public synchronized WeeklyScheduleLoader.Data getWeeklyScheduleData(Integer rootTaskId) {
        ArrayList<WeeklyScheduleLoader.ScheduleData> scheduleDatas = null;

        if (rootTaskId != null) {
            Task rootTask = mTasks.get(rootTaskId);
            Assert.assertTrue(rootTask != null);

            ExactTimeStamp now = ExactTimeStamp.getNow();

            WeeklySchedule weeklySchedule = (WeeklySchedule) rootTask.getCurrentSchedule(now);
            Assert.assertTrue(weeklySchedule != null);
            Assert.assertTrue(weeklySchedule.current(now));

            ArrayList<Pair<DayOfWeek, Time>> pairs = weeklySchedule.getDayOfWeekTimes();
            scheduleDatas = new ArrayList<>();
            for (Pair<DayOfWeek, Time> pair : pairs)
                scheduleDatas.add(new WeeklyScheduleLoader.ScheduleData(pair.first, pair.second.getTimePair()));
        }

        ArrayList<CustomTime> customTimes = getCurrentCustomTimes();
        HashMap<Integer, WeeklyScheduleLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes)
            customTimeDatas.put(customTime.getId(), new WeeklyScheduleLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new WeeklyScheduleLoader.Data(scheduleDatas, customTimeDatas);
    }

    public synchronized ShowTaskLoader.Data getShowTaskData(int taskId, Context context) {
        Assert.assertTrue(context != null);

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        ArrayList<Task> childTasks = task.getChildTasks(now);
        ArrayList<ShowTaskLoader.ChildTaskData> childTaskDatas = new ArrayList<>();
        for (Task childTask : childTasks)
            childTaskDatas.add(new ShowTaskLoader.ChildTaskData(childTask.getId(), childTask.getName(), !childTask.getChildTasks(now).isEmpty()));

        return new ShowTaskLoader.Data(task.isRootTask(now), task.getName(), task.getScheduleText(context, now), task.getId(), childTaskDatas);
    }

    public synchronized TaskListLoader.Data getTaskListData(Context context) {
        Assert.assertTrue(context != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        ArrayList<TaskListLoader.RootTaskData> rootTaskDatas = new ArrayList<>();
        for (Task task : mTasks.values())
            if (task.current(now) && task.isRootTask(now))
                rootTaskDatas.add(new TaskListLoader.RootTaskData(task.getId(), task.getName(), task.getScheduleText(context, now), !task.getChildTasks(now).isEmpty()));

        return new TaskListLoader.Data(rootTaskDatas);
    }

    public synchronized TickService.Data getTickServiceData(Context context) {
        Assert.assertTrue(context != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        ArrayList<Instance> rootInstances = getRootInstances(null, now.plusOne(), now);

        HashMap<InstanceKey, TickService.NotificationInstanceData> notificationInstanceDatas = new HashMap<>();
        for (Instance instance : rootInstances)
            if ((instance.getDone() == null) && !instance.getNotified() && instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0)
                notificationInstanceDatas.put(instance.getInstanceKey(), new TickService.NotificationInstanceData(instance.getInstanceKey(), instance.getName(), instance.getNotificationId(), instance.getDisplayText(context, now), instance.getInstanceDateTime().getTimeStamp()));

        HashMap<InstanceKey, TickService.ShownInstanceData> shownInstanceDatas = new HashMap<>();
        for (Instance instance : mExistingInstances)
            if (instance.getNotificationShown())
                shownInstanceDatas.put(instance.getInstanceKey(), new TickService.ShownInstanceData(instance.getNotificationId(), instance.getInstanceKey()));

        TimeStamp nextAlarm = null;
        for (Instance existingInstance : mExistingInstances) {
            TimeStamp instanceTimeStamp = existingInstance.getInstanceDateTime().getTimeStamp();
            if (instanceTimeStamp.toExactTimeStamp().compareTo(now) > 0)
                if (nextAlarm == null || instanceTimeStamp.compareTo(nextAlarm) < 0)
                    nextAlarm = instanceTimeStamp;
        }

        for (Task task : mTasks.values()) {
            if (task.current(now) && task.isRootTask(now)) {
                Schedule schedule = task.getCurrentSchedule(now);
                TimeStamp scheduleTimeStamp = schedule.getNextAlarm(now);
                if (scheduleTimeStamp != null) {
                    Assert.assertTrue(scheduleTimeStamp.toExactTimeStamp().compareTo(now) > 0);
                    if (nextAlarm == null || scheduleTimeStamp.compareTo(nextAlarm) < 0)
                        nextAlarm = scheduleTimeStamp;
                }
            }
        }

        return new TickService.Data(notificationInstanceDatas, shownInstanceDatas, nextAlarm);
    }

    // sets

    public synchronized void setInstanceDateTime(int dataId, InstanceKey instanceKey, Date instanceDate, TimePair instanceTimePair) {
        Assert.assertTrue(instanceKey != null);
        Assert.assertTrue(instanceDate != null);
        Assert.assertTrue(instanceTimePair != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now);

        save(dataId);
    }

    public synchronized void setInstanceAddHour(int dataId, InstanceKey instanceKey) {
        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);

        save(dataId);
    }

    public synchronized ExactTimeStamp setInstanceDone(int dataId, InstanceKey instanceKey, boolean done) {
        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setDone(done, now);

        save(dataId);

        return instance.getDone();
    }

    public synchronized void setInstancesNotified(int dataId, ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);
            Assert.assertTrue(instance != null);

            instance.setNotified(true, now);
            instance.setNotificationShown(false, now);
        }

        save(dataId);
    }

    public synchronized void setInstanceNotified(int dataId, InstanceKey instanceKey) {
        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setNotified(true, now);
        instance.setNotificationShown(false, now);

        save(dataId);
    }

    public synchronized void updateInstancesShown(int dataId, ArrayList<InstanceKey> showInstanceKeys, ArrayList<InstanceKey> hideInstanceKeys) {
        Assert.assertTrue(hideInstanceKeys != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        if (showInstanceKeys != null) {
            for (InstanceKey showInstanceKey : showInstanceKeys) {
                Assert.assertTrue(showInstanceKey != null);

                Instance showInstance = getInstance(showInstanceKey);
                Assert.assertTrue(showInstance != null);

                showInstance.setNotificationShown(true, now);
            }
        }

        for (InstanceKey hideInstanceKey : hideInstanceKeys) {
            Assert.assertTrue(hideInstanceKey != null);

            Instance hideInstance = getInstance(hideInstanceKey);
            Assert.assertTrue(hideInstance != null);

            hideInstance.setNotificationShown(false, now);
        }

        save(dataId);
    }

    public synchronized void createSingleScheduleRootTask(int dataId, String name, Date date, TimePair timePair) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue(timePair != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTask(name, now);
        Assert.assertTrue(rootTask != null);

        Time time = getTime(timePair);

        Schedule schedule = createSingleSchedule(rootTask, date, time, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public synchronized void createDailyScheduleRootTask(int dataId, String name, ArrayList<TimePair> timePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTask(name, now);
        Assert.assertTrue(rootTask != null);

        ArrayList<Time> times = getTimes(timePairs);

        Schedule schedule = createDailySchedule(rootTask, times, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public synchronized void createWeeklyScheduleRootTask(int dataId, String name, ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTask(name, now);
        Assert.assertTrue(rootTask != null);

        Schedule schedule = createWeeklySchedule(rootTask, dayOfWeekTimePairs, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public synchronized void updateSingleScheduleRootTask(int dataId, int rootTaskId, String name, Date date, TimePair timePair) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue(timePair != null);

        Task rootTask = mTasks.get(rootTaskId);
        Assert.assertTrue(rootTask != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(rootTask.current(now));
        Assert.assertTrue(rootTask.isRootTask(now));

        rootTask.setName(name);

        if (rootTask.getCurrentSchedule(now).getType() == ScheduleType.SINGLE) {
            SingleSchedule singleSchedule = (SingleSchedule) rootTask.getCurrentSchedule(now);

            Instance instance = singleSchedule.getInstance(rootTask);
            Assert.assertTrue(instance != null);

            instance.setInstanceDateTime(date, timePair, now);
        } else {
            rootTask.setScheduleEndExactTimeStamp(now);

            Schedule schedule = createSingleSchedule(rootTask, date, getTime(timePair), now);
            Assert.assertTrue(schedule != null);

            rootTask.addSchedule(schedule);
        }

        save(dataId);
    }

    public synchronized void updateDailyScheduleRootTask(int dataId, int rootTaskId, String name, ArrayList<TimePair> timePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        Task rootTask = mTasks.get(rootTaskId);
        Assert.assertTrue(rootTask != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(rootTask.current(now));
        Assert.assertTrue(rootTask.isRootTask(now));

        rootTask.setName(name);
        rootTask.setScheduleEndExactTimeStamp(now);

        ArrayList<Time> times = getTimes(timePairs);

        Schedule schedule = createDailySchedule(rootTask, times, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public synchronized void updateWeeklyScheduleRootTask(int dataId, int rootTaskId, String name, ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        Task rootTask = mTasks.get(rootTaskId);
        Assert.assertTrue(rootTask != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(rootTask.current(now));
        Assert.assertTrue(rootTask.isRootTask(now));

        rootTask.setName(name);
        rootTask.setScheduleEndExactTimeStamp(now);

        Schedule schedule = createWeeklySchedule(rootTask, dayOfWeekTimePairs, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public synchronized void createSingleScheduleJoinRootTask(int dataId, String name, Date date, TimePair timePair, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue(timePair != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTask(name, now);
        Assert.assertTrue(rootTask != null);

        Time time = getTime(timePair);
        Assert.assertTrue(time != null);

        Schedule schedule = createSingleSchedule(rootTask, date, time, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        joinTasks(rootTask, joinTaskIds, now);

        save(dataId);
    }

    public synchronized void createDailyScheduleJoinRootTask(int dataId, String name, ArrayList<TimePair> timePairs, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTask(name, now);
        Assert.assertTrue(rootTask != null);

        ArrayList<Time> times = getTimes(timePairs);

        Schedule schedule = createDailySchedule(rootTask, times, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        joinTasks(rootTask, joinTaskIds, now);

        save(dataId);
    }

    public synchronized void createWeeklyScheduleJoinRootTask(int dataId, String name, ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTask(name, now);
        Assert.assertTrue(rootTask != null);

        Schedule schedule = createWeeklySchedule(rootTask, dayOfWeekTimePairs, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        joinTasks(rootTask, joinTaskIds, now);

        save(dataId);
    }

    public synchronized void createChildTask(int parentTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task parentTask = mTasks.get(parentTaskId);
        Assert.assertTrue(parentTask != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(parentTask.current(now));

        TaskRecord childTaskRecord = mPersistenceManager.createTaskRecord(name, now);
        Assert.assertTrue(childTaskRecord != null);

        Task childTask = new Task(this, childTaskRecord);
        Assert.assertTrue(!mTasks.containsKey(childTask.getId()));
        mTasks.put(childTask.getId(), childTask);

        createTaskHierarchy(parentTask, childTask, now);

        save(0);
    }

    public synchronized void updateChildTask(int dataId, int childTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task childTask = mTasks.get(childTaskId);
        Assert.assertTrue(childTask != null);

        childTask.setName(name);

        save(dataId);
    }

    public synchronized void setTaskEndTimeStamp(int dataId, int taskId) {
        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(task.current(now));

        task.setEndExactTimeStamp(now);

        save(dataId);
    }

    public synchronized void setTaskEndTimeStamps(int dataId, ArrayList<Integer> taskIds) {
        Assert.assertTrue(taskIds != null);
        Assert.assertTrue(!taskIds.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (int taskId : taskIds) {
            Task task = mTasks.get(taskId);
            Assert.assertTrue(task != null);

            Assert.assertTrue(task.current(now));

            task.setEndExactTimeStamp(now);
        }

        save(dataId);
    }

    public synchronized void createCustomTime(String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
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

        save(0);
    }

    public synchronized void updateCustomTime(int dataId, int customTimeId, String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
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

    public synchronized void setCustomTimeCurrent(int dataId, int customTimeId) {
        CustomTime customTime = mCustomTimes.get(customTimeId);
        Assert.assertTrue(customTime != null);

        customTime.setCurrent();

        save(dataId);
    }

    // internal

    ArrayList<Instance> getExistingInstances(Task task) {
        Assert.assertTrue(task != null);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Instance instance : mExistingInstances) {
            Assert.assertTrue(instance != null);
            if (instance.getTaskId() == task.getId())
                instances.add(instance);
        }

        return instances;
    }

    Instance getInstance(Task task, DateTime scheduleDateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDateTime != null);

        ArrayList<Instance> taskInstances = getExistingInstances(task);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Instance instance : taskInstances) {
            Assert.assertTrue(instance != null);
            if (instance.getScheduleDateTime().compareTo(scheduleDateTime) == 0)
                instances.add(instance);
        }

        if (!instances.isEmpty()) {
            Assert.assertTrue(instances.size() == 1);
            return instances.get(0);
        } else {
            return new Instance(this, task, scheduleDateTime);
        }
    }

    private ArrayList<Instance> getRootInstances(ExactTimeStamp startExactTimeStamp, ExactTimeStamp endExactTimeStamp, ExactTimeStamp now) {
        Assert.assertTrue(endExactTimeStamp != null);
        Assert.assertTrue(startExactTimeStamp == null || startExactTimeStamp.compareTo(endExactTimeStamp) < 0);
        Assert.assertTrue(now != null);

        HashSet<Instance> allInstances = new HashSet<>();
        allInstances.addAll(mExistingInstances);

        for (Task task : mTasks.values())
            allInstances.addAll(task.getInstances(startExactTimeStamp, endExactTimeStamp));

        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        ExactTimeStamp twentyFourHoursAgo = new ExactTimeStamp(calendar);

        ArrayList<Instance> rootInstances = new ArrayList<>();
        for (Instance instance : allInstances)
            if (instance.isRootInstance(now) && (instance.getDone() == null || (instance.getDone().compareTo(twentyFourHoursAgo) > 0)))
                rootInstances.add(instance);

        return rootInstances;
    }

    InstanceRecord createInstanceRecord(Task task, Instance instance, DateTime scheduleDateTime, ExactTimeStamp now) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(instance != null);
        Assert.assertTrue(scheduleDateTime != null);
        Assert.assertTrue(now != null);

        mExistingInstances.add(instance);

        return mPersistenceManager.createInstanceRecord(task, scheduleDateTime, now);
    }

    private DateTime getDateTime(Date date, TimePair timePair) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(timePair != null);

        Time time = getTime(timePair);
        Assert.assertTrue(time != null);

        return new DateTime(date, time);
    }

    private Instance getInstance(InstanceKey instanceKey) {
        Task task = mTasks.get(instanceKey.TaskId);
        Assert.assertTrue(task != null);

        DateTime scheduleDateTime = getDateTime(instanceKey.ScheduleDate, instanceKey.ScheduleTimePair);
        Assert.assertTrue(scheduleDateTime != null);

        Instance instance = getInstance(task, scheduleDateTime);
        Assert.assertTrue(instance != null);

        return instance;
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

        SingleSchedule singleSchedule = new SingleSchedule(scheduleRecord, rootTask, this);

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

    private Task createRootTask(String name, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(startExactTimeStamp != null);

        TaskRecord taskRecord = mPersistenceManager.createTaskRecord(name, startExactTimeStamp);
        Assert.assertTrue(taskRecord != null);

        Task rootTask = new Task(this, taskRecord);

        Assert.assertTrue(!mTasks.containsKey(rootTask.getId()));
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    private ArrayList<Time> getTimes(ArrayList<TimePair> timePairs) {
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        ArrayList<Time> times = new ArrayList<>();
        for (TimePair timePair : timePairs)
            times.add(getTime(timePair));

        return times;
    }

    private Time getTime(TimePair timePair) {
        Assert.assertTrue(timePair != null);

        if (timePair.CustomTimeId != null) {
            Assert.assertTrue(timePair.HourMinute == null);

            CustomTime customTime = mCustomTimes.get(timePair.CustomTimeId);
            Assert.assertTrue(customTime != null);

            return customTime;
        } else {
            Assert.assertTrue(timePair.HourMinute != null);
            return new NormalTime(timePair.HourMinute);
        }
    }

    private void joinTasks(Task rootTask, ArrayList<Integer> joinTaskIds, ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(rootTask.current(exactTimeStamp));
        Assert.assertTrue(rootTask.isRootTask(exactTimeStamp));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        for (int joinTaskId : joinTaskIds) {
            Task joinTask = mTasks.get(joinTaskId);
            Assert.assertTrue(joinTask != null);
            Assert.assertTrue(joinTask.current(exactTimeStamp));
            Assert.assertTrue(joinTask.isRootTask(exactTimeStamp));

            joinTask.setScheduleEndExactTimeStamp(exactTimeStamp);

            createTaskHierarchy(rootTask, joinTask, exactTimeStamp);
        }
    }

    private void createTaskHierarchy(Task parentTask, Task childTask, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(startExactTimeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(startExactTimeStamp));
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(childTask.current(startExactTimeStamp));

        TaskHierarchyRecord taskHierarchyRecord = mPersistenceManager.createTaskHierarchyRecord(parentTask, childTask, startExactTimeStamp);
        Assert.assertTrue(taskHierarchyRecord != null);

        TaskHierarchy taskHierarchy = new TaskHierarchy(taskHierarchyRecord, parentTask, childTask);
        Assert.assertTrue(!mTaskHierarchies.containsKey(taskHierarchy.getId()));
        mTaskHierarchies.put(taskHierarchy.getId(), taskHierarchy);
    }

    private SingleSchedule createSingleSchedule(Task rootTask, Date date, Time time, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(date != null);
        Assert.assertTrue(time != null);
        Assert.assertTrue(startExactTimeStamp != null);
        Assert.assertTrue(new DateTime(date, time).getTimeStamp().toExactTimeStamp().compareTo(startExactTimeStamp) >= 0);

        ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.SINGLE, startExactTimeStamp);
        Assert.assertTrue(scheduleRecord != null);

        SingleSchedule singleSchedule = new SingleSchedule(scheduleRecord, rootTask, this);

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = mPersistenceManager.createSingleScheduleDateTimeRecord(singleSchedule, date, time);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        singleSchedule.setSingleScheduleDateTime(new SingleScheduleDateTime(this, singleScheduleDateTimeRecord));

        return singleSchedule;
    }

    private DailySchedule createDailySchedule(Task rootTask, ArrayList<Time> times, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(times != null);
        Assert.assertTrue(!times.isEmpty());
        Assert.assertTrue(startExactTimeStamp != null);
        Assert.assertTrue(rootTask.current(startExactTimeStamp));

        ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.DAILY, startExactTimeStamp);
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

    private WeeklySchedule createWeeklySchedule(Task rootTask, ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());
        Assert.assertTrue(startExactTimeStamp != null);
        Assert.assertTrue(rootTask.current(startExactTimeStamp));

        ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.WEEKLY, startExactTimeStamp);
        Assert.assertTrue(scheduleRecord != null);

        WeeklySchedule weeklySchedule = new WeeklySchedule(scheduleRecord, rootTask);

        for (Pair<DayOfWeek, TimePair> dayOfWeekTimePair : dayOfWeekTimePairs) {
            Assert.assertTrue(dayOfWeekTimePair != null);

            DayOfWeek dayOfWeek = dayOfWeekTimePair.first;
            Time time = getTime(dayOfWeekTimePair.second);

            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(time != null);

            WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = mPersistenceManager.createWeeklyScheduleDayOfWeekTimeRecord(weeklySchedule, dayOfWeek, time);
            Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecord != null);

            weeklySchedule.addWeeklyScheduleDayOfWeekTime(new WeeklyScheduleDayOfWeekTime(this, weeklyScheduleDayOfWeekTimeRecord));
        }

        return weeklySchedule;
    }

    ArrayList<Task> getChildTasks(Task parentTask, ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(exactTimeStamp));

        ArrayList<TaskHierarchy> taskHierarchies = getTaskHierarchies(parentTask);
        ArrayList<Task> childTasks = new ArrayList<>();
        for (TaskHierarchy taskHierarchy : taskHierarchies)
            if (taskHierarchy.current(exactTimeStamp) && taskHierarchy.getChildTask().current(exactTimeStamp))
                childTasks.add(taskHierarchy.getChildTask());

        Collections.sort(childTasks, new Comparator<Task>() {
            @Override
            public int compare(Task lhs, Task rhs) {
                return  new Integer(lhs.getId()).compareTo(rhs.getId());
            }
        });

        return childTasks;
    }

    ArrayList<TaskHierarchy> getTaskHierarchies(Task parentTask) {
        Assert.assertTrue(parentTask != null);

        ArrayList<TaskHierarchy> taskHierarchies = new ArrayList<>();
        for (TaskHierarchy taskHierarchy : mTaskHierarchies.values())
            if (taskHierarchy.getParentTask() == parentTask)
                taskHierarchies.add(taskHierarchy);
        return taskHierarchies;
    }

    Task getParentTask(Task childTask, ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(childTask.current(exactTimeStamp));

        TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, exactTimeStamp);
        if (parentTaskHierarchy == null) {
            return null;
        } else {
            Assert.assertTrue(parentTaskHierarchy.current(exactTimeStamp));
            Task parentTask = parentTaskHierarchy.getParentTask();
            Assert.assertTrue(parentTask.current(exactTimeStamp));
            return parentTask;
        }
    }

    private TaskHierarchy getParentTaskHierarchy(Task childTask, ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(childTask.current(exactTimeStamp));

        ArrayList<TaskHierarchy> taskHierarchies = new ArrayList<>();
        for (TaskHierarchy taskHierarchy : mTaskHierarchies.values()) {
            Assert.assertTrue(taskHierarchy != null);

            if (!taskHierarchy.current(exactTimeStamp))
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

    void setParentHierarchyEndTimeStamp(Task childTask, ExactTimeStamp exactEndTimeStamp) {
        Assert.assertTrue(childTask != null);
        Assert.assertTrue(exactEndTimeStamp != null);
        Assert.assertTrue(childTask.current(exactEndTimeStamp));

        TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, exactEndTimeStamp);
        if (parentTaskHierarchy != null) {
            Assert.assertTrue(parentTaskHierarchy.current(exactEndTimeStamp));
            parentTaskHierarchy.setEndExactTimeStamp(exactEndTimeStamp);
        }
    }

    CustomTime getCustomTime(int customTimeId) {
        Assert.assertTrue(mCustomTimes.containsKey(customTimeId));
        return mCustomTimes.get(customTimeId);
    }

    private ArrayList<CustomTime> getCurrentCustomTimes() {
        ArrayList<CustomTime> customTimes = new ArrayList<>();
        for (CustomTime customTime : mCustomTimes.values())
            if (customTime.getCurrent())
                customTimes.add(customTime);
        return customTimes;
    }

}
