package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.loaders.EditInstanceLoader;
import com.krystianwsul.checkme.loaders.EditInstancesLoader;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.loaders.ShowCustomTimeLoader;
import com.krystianwsul.checkme.loaders.ShowCustomTimesLoader;
import com.krystianwsul.checkme.loaders.ShowGroupLoader;
import com.krystianwsul.checkme.loaders.ShowInstanceLoader;
import com.krystianwsul.checkme.loaders.ShowTaskLoader;
import com.krystianwsul.checkme.loaders.TaskListLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.persistencemodel.CustomTimeRecord;
import com.krystianwsul.checkme.persistencemodel.DailyScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.InstanceRecord;
import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.SingleScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.TaskHierarchyRecord;
import com.krystianwsul.checkme.persistencemodel.TaskRecord;
import com.krystianwsul.checkme.persistencemodel.WeeklyScheduleRecord;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMili;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DomainFactory {
    private static DomainFactory sDomainFactory;

    private final PersistenceManger mPersistenceManager;

    private final HashMap<Integer, CustomTime> mCustomTimes = new HashMap<>();
    private final HashMap<Integer, Task> mTasks = new HashMap<>();
    private final HashMap<Integer, TaskHierarchy> mTaskHierarchies = new HashMap<>();
    private final ArrayList<Instance> mExistingInstances = new ArrayList<>();

    private static ExactTimeStamp sStart;
    private static ExactTimeStamp sRead;
    private static ExactTimeStamp sStop;

    public static synchronized DomainFactory getDomainFactory(Context context) {
        Assert.assertTrue(context != null);

        if (sDomainFactory == null) {
            sStart = ExactTimeStamp.getNow();

            sDomainFactory = new DomainFactory(context);

            sRead = ExactTimeStamp.getNow();

            sDomainFactory.initialize();

            sStop = ExactTimeStamp.getNow();
        }

        return sDomainFactory;
    }

    private DomainFactory(Context context) {
        mPersistenceManager = PersistenceManger.getInstance(context);
        Assert.assertTrue(mPersistenceManager != null);
    }

    DomainFactory(PersistenceManger persistenceManger) {
        Assert.assertTrue(persistenceManger != null);

        mPersistenceManager = persistenceManger;
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

    public long getReadMillis() {
        return (sRead.getLong() - sStart.getLong());
    }

    public long getInstantiateMillis() {
        return (sStop.getLong() - sRead.getLong());
    }

    public synchronized void reset() {
        sDomainFactory = null;
        mPersistenceManager.reset();

        ObserverHolder.getObserverHolder().notifyDomainObservers(new ArrayList<>());

        ObserverHolder.getObserverHolder().clear();
    }

    public int getTaskCount() {
        return mTasks.size();
    }

    public int getInstanceCount() {
        return mExistingInstances.size();
    }

    public int getCustomTimeCount() {
        return mCustomTimes.size();
    }

    private void save(int dataId) {
        ArrayList<Integer> dataIds = new ArrayList<>();
        dataIds.add(dataId);
        save(dataIds);
    }

    private void save(ArrayList<Integer> dataIds) {
        Assert.assertTrue(dataIds != null);

        mPersistenceManager.save();
        ObserverHolder.getObserverHolder().notifyDomainObservers(dataIds);
    }

    // gets

    @SuppressWarnings("EmptyMethod")
    private void fakeDelay() {
        /*
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }
        */
    }

    public synchronized EditInstanceLoader.Data getEditInstanceData(InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getEditInstanceData");

        Assert.assertTrue(instanceKey != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<Integer, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getId, customTime -> customTime));

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);
        Assert.assertTrue(instance.isRootInstance(now));
        Assert.assertTrue(instance.getDone() == null);

        if (instance.getInstanceTimePair().CustomTimeId != null) {
            CustomTime customTime = mCustomTimes.get(instance.getInstanceTimePair().CustomTimeId);
            Assert.assertTrue(customTime != null);

            currentCustomTimes.put(customTime.getId(), customTime);
        }

        TreeMap<Integer, EditInstanceLoader.CustomTimeData> customTimeDatas = new TreeMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getId(), new EditInstanceLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstanceLoader.Data(instance.getInstanceKey(), instance.getInstanceDate(), instance.getInstanceTimePair(), instance.getName(), customTimeDatas);
    }

    public synchronized EditInstancesLoader.Data getEditInstancesData(ArrayList<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getEditInstancesData");

        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<Integer, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getId, customTime -> customTime));

        HashMap<InstanceKey, EditInstancesLoader.InstanceData> instanceDatas = new HashMap<>();

        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);
            Assert.assertTrue(instance != null);
            Assert.assertTrue(instance.isRootInstance(now));
            Assert.assertTrue(instance.getDone() == null);

            instanceDatas.put(instanceKey, new EditInstancesLoader.InstanceData(instance.getInstanceDate(), instance.getName()));

            if (instance.getInstanceTimePair().CustomTimeId != null) {
                CustomTime customTime = mCustomTimes.get(instance.getInstanceTimePair().CustomTimeId);
                Assert.assertTrue(customTime != null);

                currentCustomTimes.put(customTime.getId(), customTime);
            }
        }

        TreeMap<Integer, EditInstancesLoader.CustomTimeData> customTimeDatas = new TreeMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getId(), new EditInstancesLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstancesLoader.Data(instanceDatas, customTimeDatas);
    }

    public synchronized ShowCustomTimeLoader.Data getShowCustomTimeData(int customTimeId) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowCustomTimeData");

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
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowCustomTimesData");

        List<CustomTime> currentCustomTimes = getCurrentCustomTimes();

        ArrayList<ShowCustomTimesLoader.CustomTimeData> entries = new ArrayList<>();
        for (CustomTime customTime : currentCustomTimes) {
            Assert.assertTrue(customTime != null);

            entries.add(new ShowCustomTimesLoader.CustomTimeData(customTime.getId(), customTime.getName()));
        }

        return new ShowCustomTimesLoader.Data(entries);
    }

    public synchronized GroupListLoader.Data getGroupListData(Context context, int position, MainActivity.TimeRange timeRange) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Assert.assertTrue(position >= 0);
        Assert.assertTrue(timeRange != null);

        ExactTimeStamp startExactTimeStamp;
        ExactTimeStamp endExactTimeStamp;

        if (position == 0) {
            startExactTimeStamp = null;
        } else {
            Calendar startCalendar = Calendar.getInstance();

            switch (timeRange) {
                case DAY:
                    startCalendar.add(Calendar.DATE, position);
                    break;
                case WEEK:
                    startCalendar.add(Calendar.WEEK_OF_YEAR, position);
                    startCalendar.set(Calendar.DAY_OF_WEEK, startCalendar.getFirstDayOfWeek());
                    break;
                case MONTH:
                    startCalendar.add(Calendar.MONTH, position);
                    startCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            startExactTimeStamp = new ExactTimeStamp(new Date(startCalendar), new HourMili(0, 0, 0, 0));
        }

        Calendar endCalendar = Calendar.getInstance();

        switch (timeRange) {
            case DAY:
                endCalendar.add(Calendar.DATE, position + 1);
                break;
            case WEEK:
                endCalendar.add(Calendar.WEEK_OF_YEAR, position + 1);
                endCalendar.set(Calendar.DAY_OF_WEEK, endCalendar.getFirstDayOfWeek());
                break;
            case MONTH:
                endCalendar.add(Calendar.MONTH, position + 1);
                endCalendar.set(Calendar.DAY_OF_MONTH, 1);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        endExactTimeStamp = new ExactTimeStamp(new Date(endCalendar), new HourMili(0, 0, 0, 0));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<Instance> currentInstances = getRootInstances(startExactTimeStamp, endExactTimeStamp, now);

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        List<GroupListLoader.TaskData> taskDatas = null;
        if (position == 0) {
            taskDatas = Stream.of(mTasks.values())
                    .filter(task -> task.current(now))
                    .filter(task -> task.isVisible(now))
                    .filter(task -> task.isRootTask(now))
                    .filter(task -> task.getCurrentSchedules(now).isEmpty())
                    .map(task -> new GroupListLoader.TaskData(task.getId(), task.getName(), getChildTaskDatas(task, now), true))
                    .collect(Collectors.toList());
        }

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null, taskDatas);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = mTasks.get(instance.getTaskId());
            Assert.assertTrue(task != null);

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), new WeakReference<>(data), instance.getInstanceDateTime().getTime().getTimePair());
            instanceData.setChildren(getChildInstanceDatas(instance, now, new WeakReference<>(instanceData)));
            instanceDatas.put(instanceData.InstanceKey, instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    private List<GroupListLoader.TaskData> getChildTaskDatas(Task parentTask, ExactTimeStamp now) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(now != null);

        return Stream.of(parentTask.getChildTasks(now))
                .map(childTask -> new GroupListLoader.TaskData(childTask.getId(), childTask.getName(), getChildTaskDatas(childTask, now), false))
                .collect(Collectors.toList());
    }

    public synchronized ShowGroupLoader.Data getShowGroupData(Context context, TimeStamp timeStamp) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowGroupData");

        Assert.assertTrue(context != null);
        Assert.assertTrue(timeStamp != null);

        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<Instance> rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now);

        List<Instance> currentInstances = Stream.of(rootInstances)
                .filter(instance -> instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                .collect(Collectors.toList());

        Date date = timeStamp.getDate();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        HourMinute hourMinute = timeStamp.getHourMinute();

        Time time = null;
        for (CustomTime customTime : getCurrentCustomTimes())
            if (customTime.getHourMinute(dayOfWeek).equals(hourMinute))
                time = customTime;
        if (time == null)
            time = new NormalTime(hourMinute);

        String displayText = new DateTime(date, time).getDisplayText(context);

        return new ShowGroupLoader.Data(displayText, !currentInstances.isEmpty());
    }

    public synchronized GroupListLoader.Data getGroupListData(Context context, TimeStamp timeStamp) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Assert.assertTrue(context != null);
        Assert.assertTrue(timeStamp != null);

        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<Instance> rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now);

        List<Instance> currentInstances = Stream.of(rootInstances)
                .filter((Instance instance) -> instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                .collect(Collectors.toList());

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = mTasks.get(instance.getTaskId());
            Assert.assertTrue(task != null);

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), null, instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), new WeakReference<>(data), instance.getInstanceDateTime().getTime().getTimePair());
            instanceData.setChildren(getChildInstanceDatas(instance, now, new WeakReference<>(instanceData)));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    public synchronized GroupListLoader.Data getGroupListData(Context context, InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        Task task = mTasks.get(instance.getTaskId());
        Assert.assertTrue(task != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, task.current(now), null);

        ArrayList<Instance> childInstances = instance.getChildInstances(now);
        for (Instance childInstance : childInstances) {
            Task childTask = mTasks.get(childInstance.getTaskId());
            Assert.assertTrue(childTask != null);

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), new WeakReference<>(data), childInstance.getInstanceDateTime().getTime().getTimePair());
            instanceData.setChildren(getChildInstanceDatas(childInstance, now, new WeakReference<>(instanceData)));
            instanceDatas.put(childInstance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    public synchronized GroupListLoader.Data getGroupListData(Context context, ArrayList<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        ArrayList<Instance> instances = new ArrayList<>();
        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);
            Assert.assertTrue(instance != null);

            if (instance.isRootInstance(now))
                instances.add(instance);
        }

        Collections.sort(instances, (Instance lhs, Instance rhs) -> lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime()));

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : instances) {
            Task task = mTasks.get(instance.getTaskId());
            Assert.assertTrue(task != null);

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), new WeakReference<>(data), instance.getInstanceDateTime().getTime().getTimePair());
            instanceData.setChildren(getChildInstanceDatas(instance, now, new WeakReference<>(instanceData)));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    public synchronized ShowInstanceLoader.Data getShowInstanceData(Context context, InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowInstanceData");

        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        Task task = mTasks.get(instance.getTaskId());
        Assert.assertTrue(task != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);
        return new ShowInstanceLoader.Data(instance.getInstanceKey(), instance.getName(), instance.getDisplayText(context, now), instance.getDone() != null, task.current(now), instance.isRootInstance(now), isRootTask);
    }

    public synchronized CreateTaskLoader.Data getCreateChildTaskData(Integer taskId, Context context, List<Integer> excludedTaskIds) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getCreateChildTaskData");

        Assert.assertTrue(context != null);
        Assert.assertTrue(excludedTaskIds != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<Integer, CustomTime> customTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getId, customTime -> customTime));

        CreateTaskLoader.TaskData taskData = null;
        if (taskId != null) {
            Task task = mTasks.get(taskId);
            Assert.assertTrue(task != null);

            Integer parentTaskId;
            List<CreateTaskLoader.ScheduleData> scheduleDatas = null;

            if (task.isRootTask(now)) {
                List<Schedule> schedules = task.getCurrentSchedules(now);
                Assert.assertTrue(schedules != null);

                parentTaskId = null;

                if (!schedules.isEmpty()) {
                    scheduleDatas = new ArrayList<>();

                    for (Schedule schedule : schedules) {
                        Assert.assertTrue(schedule != null);
                        Assert.assertTrue(schedule.current(now));

                        switch (schedule.getType()) {
                            case SINGLE: {
                                SingleSchedule singleSchedule = (SingleSchedule) schedule;

                                Pair<Date, Time> pair = new Pair<>(singleSchedule.getDate(), singleSchedule.getTime());

                                scheduleDatas.add(new CreateTaskLoader.SingleScheduleData(pair.first, pair.second.getTimePair()));

                                CustomTime weeklyCustomTime = pair.second.getPair().first;
                                if (weeklyCustomTime != null)
                                    customTimes.put(weeklyCustomTime.getId(), weeklyCustomTime);
                                break;
                            }
                            case DAILY: {
                                DailySchedule dailySchedule = (DailySchedule) schedule;

                                Time time = dailySchedule.getTime();
                                Assert.assertTrue(time != null);

                                scheduleDatas.add(new CreateTaskLoader.DailyScheduleData(time.getTimePair()));

                                CustomTime dailyCustomTime = time.getPair().first;
                                if (dailyCustomTime != null)
                                    customTimes.put(dailyCustomTime.getId(), dailyCustomTime);

                                break;
                            }
                            case WEEKLY: {
                                WeeklySchedule weeklySchedule = (WeeklySchedule) schedule;

                                Pair<DayOfWeek, Time> pair = weeklySchedule.getDayOfWeekTime();
                                Assert.assertTrue(pair != null);

                                scheduleDatas.add(new CreateTaskLoader.WeeklyScheduleData(pair.first, pair.second.getTimePair()));

                                CustomTime weeklyCustomTime = pair.second.getPair().first;
                                if (weeklyCustomTime != null)
                                    customTimes.put(weeklyCustomTime.getId(), weeklyCustomTime);

                                break;
                            }
                            default: {
                                throw new UnsupportedOperationException();
                            }
                        }

                    }
                }
            } else {
                Task parentTask = task.getParentTask(now);
                Assert.assertTrue(parentTask != null);

                parentTaskId = parentTask.getId();
            }

            taskData = new CreateTaskLoader.TaskData(task.getName(), parentTaskId, scheduleDatas);
        }

        TreeMap<Integer, CreateTaskLoader.TaskTreeData> taskDatas = getTaskDatas(context, now, excludedTaskIds);
        Assert.assertTrue(taskDatas != null);

        HashMap<Integer, CreateTaskLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes.values())
            customTimeDatas.put(customTime.getId(), new CreateTaskLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new CreateTaskLoader.Data(taskData, taskDatas, customTimeDatas);
    }

    public synchronized ShowTaskLoader.Data getShowTaskData(int taskId, Context context) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowTaskData");

        Assert.assertTrue(context != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);
        Assert.assertTrue(task.current(now));

        return new ShowTaskLoader.Data(task.isRootTask(now), task.getName(), task.getScheduleText(context, now), task.getId());
    }

    public synchronized TaskListLoader.Data getTaskListData(Context context, Integer taskId) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getTaskListData");

        Assert.assertTrue(context != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        return getTaskListData(now, context, taskId);
    }

    TaskListLoader.Data getTaskListData(ExactTimeStamp now, Context context, Integer taskId) {
        Assert.assertTrue(now != null);
        Assert.assertTrue(context != null);

        List<Task> tasks;

        if (taskId != null) {
            Task parentTask = mTasks.get(taskId);
            Assert.assertTrue(parentTask != null);

            tasks = parentTask.getChildTasks(now);
            Assert.assertTrue(tasks != null);
        } else {
            tasks = new ArrayList<>();
            for (Task rootTask : mTasks.values()) {
                if (!rootTask.current(now))
                    continue;

                if (!rootTask.isVisible(now))
                    continue;

                if (!rootTask.isRootTask(now))
                    continue;

                tasks.add(rootTask);
            }
        }

        Collections.sort(tasks, (Task lhs, Task rhs) -> Integer.valueOf(lhs.getId()).compareTo(rhs.getId()));
        if (taskId == null)
            Collections.reverse(tasks);

        List<TaskListLoader.TaskData> taskDatas = Stream.of(tasks)
                .map(task -> new TaskListLoader.TaskData(task.getId(), task.getName(), task.getScheduleText(context, now), getChildTaskDatas(task, now, context), task.isRootTask(now)))
                .collect(Collectors.toList());

        return new TaskListLoader.Data(taskDatas);
    }

    public synchronized TickService.Data getTickServiceData(Context context) {
        MyCrashlytics.log("DomainFactory.getTickServiceData");

        Assert.assertTrue(context != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<Instance> rootInstances = getRootInstances(null, now.plusOne(), now); // 24 hack

        Map<InstanceKey, TickService.NotificationInstanceData> notificationInstanceDatas = Stream.of(rootInstances)
                .filter(instance -> (instance.getDone() == null) && !instance.getNotified() && instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0)
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> {
                    List<Instance> childInstances = instance.getChildInstances(now);
                    Assert.assertTrue(childInstances != null);

                    Stream<Instance> notDone = Stream.of(childInstances)
                            .filter(childInstance -> childInstance.getDone() == null)
                            .sortBy(Instance::getTaskId);

                    Stream<Instance> done = Stream.of(childInstances)
                            .filter(childInstance -> childInstance.getDone() != null)
                            .sortBy(childInstance -> -childInstance.getDone().getLong());

                    List<String> children = Stream.concat(notDone, done)
                            .map(Instance::getName)
                            .collect(Collectors.toList());

                    return new TickService.NotificationInstanceData(instance.getInstanceKey(), instance.getName(), instance.getNotificationId(), instance.getDisplayText(context, now), instance.getInstanceDateTime().getTimeStamp(), children);
                }));

        Map<InstanceKey, TickService.ShownInstanceData> shownInstanceDatas = Stream.of(mExistingInstances)
                .filter(Instance::getNotificationShown)
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> new TickService.ShownInstanceData(instance.getNotificationId(), instance.getInstanceKey())));

        TimeStamp nextAlarm = null;
        for (Instance existingInstance : mExistingInstances) {
            TimeStamp instanceTimeStamp = existingInstance.getInstanceDateTime().getTimeStamp();
            if (instanceTimeStamp.toExactTimeStamp().compareTo(now) > 0)
                if (nextAlarm == null || instanceTimeStamp.compareTo(nextAlarm) < 0)
                    nextAlarm = instanceTimeStamp;
        }

        for (Task task : mTasks.values()) {
            if (task.current(now) && task.isRootTask(now)) {
                List<Schedule> schedules = task.getCurrentSchedules(now);
                Assert.assertTrue(schedules != null);

                Optional<TimeStamp> optional = Stream.of(schedules)
                        .map(schedule -> schedule.getNextAlarm(now))
                        .filter(timeStamp -> timeStamp != null)
                        .min(TimeStamp::compareTo);

                if (optional.isPresent()) {
                    TimeStamp scheduleTimeStamp = optional.get();
                    Assert.assertTrue(scheduleTimeStamp != null);
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
        MyCrashlytics.log("DomainFactory.setInstanceDateTime");

        Assert.assertTrue(instanceKey != null);
        Assert.assertTrue(instanceDate != null);
        Assert.assertTrue(instanceTimePair != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now);

        save(dataId);
    }

    public synchronized void setInstancesDateTime(int dataId, Set<InstanceKey> instanceKeys, Date instanceDate, TimePair instanceTimePair) {
        MyCrashlytics.log("DomainFactory.setInstancesDateTime");

        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(instanceKeys.size() > 1);
        Assert.assertTrue(instanceDate != null);
        Assert.assertTrue(instanceTimePair != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);
            Assert.assertTrue(instance != null);

            instance.setInstanceDateTime(instanceDate, instanceTimePair, now);
        }

        save(dataId);
    }

    public synchronized void setInstanceAddHour(int dataId, InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHour");

        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);
        instance.setNotificationShown(false, now);

        save(dataId);
    }

    public synchronized void setInstanceNotificationDone(int dataId, InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotificationDone");

        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setDone(true, now);
        instance.setNotificationShown(false, now);
        instance.setNotified(true, now);

        save(dataId);
    }

    public synchronized ExactTimeStamp setInstanceDone(int dataId, InstanceKey instanceKey, boolean done) {
        MyCrashlytics.log("DomainFactory.setInstanceDone");

        Assert.assertTrue(instanceKey != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Instance instance = setInstanceDone(now, instanceKey, done);
        Assert.assertTrue(instance != null);

        save(dataId);

        return instance.getDone();
    }

    Instance setInstanceDone(ExactTimeStamp now, InstanceKey instanceKey, boolean done) {
        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        instance.setDone(done, now);

        return instance;
    }

    public synchronized void setInstancesNotified(int dataId, ArrayList<InstanceKey> instanceKeys) {
        MyCrashlytics.log("DomainFactory.setInstancesNotified");

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
        MyCrashlytics.log("DomainFactory.setInstanceNotified");

        Assert.assertTrue(instanceKey != null);

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setNotified(true, now);
        instance.setNotificationShown(false, now);

        save(dataId);
    }

    public synchronized void updateInstancesShown(int dataId, List<InstanceKey> showInstanceKeys, List<InstanceKey> hideInstanceKeys) {
        MyCrashlytics.log("DomainFactory.updateInstancesShown");

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

    Task createSingleScheduleRootTask(ExactTimeStamp now, String name, Date date, TimePair timePair) {
        Assert.assertTrue(now != null);
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue(timePair != null);

        Task rootTask = createRootTaskHelper(name, now);
        Assert.assertTrue(rootTask != null);

        Time time = getTime(timePair);

        Schedule schedule = createSingleSchedule(rootTask, date, time, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        return rootTask;
    }

    public synchronized void createScheduleRootTask(int dataId, String name, List<CreateTaskLoader.ScheduleData> scheduleDatas) {
        MyCrashlytics.log("DomainFactory.createScheduleRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(scheduleDatas != null);
        Assert.assertTrue(!scheduleDatas.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTaskHelper(name, now);
        Assert.assertTrue(rootTask != null);

        List<Schedule> schedules = createSchedules(rootTask, scheduleDatas, now);
        Assert.assertTrue(schedules != null);
        Assert.assertTrue(!schedules.isEmpty());

        rootTask.addSchedules(schedules);

        save(dataId);
    }

    public synchronized void updateScheduleTask(int dataId, int taskId, String name, List<CreateTaskLoader.ScheduleData> scheduleDatas) {
        MyCrashlytics.log("DomainFactory.updateScheduleTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(scheduleDatas != null);
        Assert.assertTrue(!scheduleDatas.isEmpty());

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(task.current(now));

        task.setName(name);

        if (task.isRootTask(now)) {
            List<Schedule> schedules = task.getCurrentSchedules(now);
            Assert.assertTrue(schedules != null);

            Stream.of(schedules)
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));
        } else {
            TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(taskHierarchy != null);

            taskHierarchy.setEndExactTimeStamp(now);
        }

        List<Schedule> schedules = createSchedules(task, scheduleDatas, now);
        Assert.assertTrue(schedules != null);
        Assert.assertTrue(!schedules.isEmpty());

        task.addSchedules(schedules);

        save(dataId);
    }

    public synchronized void createScheduleJoinRootTask(int dataId, String name, List<CreateTaskLoader.ScheduleData> scheduleDatas, List<Integer> joinTaskIds) {
        MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(scheduleDatas != null);
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTaskHelper(name, now);
        Assert.assertTrue(rootTask != null);

        List<Schedule> schedules = createSchedules(rootTask, scheduleDatas, now);
        Assert.assertTrue(schedules != null);
        Assert.assertTrue(!schedules.isEmpty());

        rootTask.addSchedules(schedules);

        joinTasks(rootTask, joinTaskIds, now);

        save(dataId);
    }

    public synchronized void createChildTask(int dataId, int parentTaskId, String name) {
        MyCrashlytics.log("DomainFactory.createChildTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        createChildTask(now, parentTaskId, name);

        save(dataId);
    }

    Task createChildTask(ExactTimeStamp now, int parentTaskId, String name) {
        Assert.assertTrue(now != null);
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task parentTask = mTasks.get(parentTaskId);
        Assert.assertTrue(parentTask != null);

        Assert.assertTrue(parentTask.current(now));

        TaskRecord childTaskRecord = mPersistenceManager.createTaskRecord(name, now);
        Assert.assertTrue(childTaskRecord != null);

        Task childTask = new Task(this, childTaskRecord);
        Assert.assertTrue(!mTasks.containsKey(childTask.getId()));
        mTasks.put(childTask.getId(), childTask);

        createTaskHierarchy(parentTask, childTask, now);

        return childTask;
    }

    public synchronized void createJoinChildTask(int dataId, int parentTaskId, String name, List<Integer> joinTaskIds) {
        MyCrashlytics.log("DomainFactory.createJoinChildTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

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

        joinTasks(childTask, joinTaskIds, now);

        save(dataId);
    }

    public synchronized void updateChildTask(int dataId, int taskId, String name, int parentTaskId) {
        MyCrashlytics.log("DomainFactory.updateChildTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        task.setName(name);

        Task newParentTask = mTasks.get(parentTaskId);
        Assert.assertTrue(newParentTask != null);

        Task oldParentTask = task.getParentTask(now);
        if (oldParentTask == null) {
            List<Schedule> schedules = task.getCurrentSchedules(now);
            Assert.assertTrue(schedules != null);

            Stream.of(schedules)
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));

            createTaskHierarchy(newParentTask, task, now);
        } else if (oldParentTask != newParentTask) {
            TaskHierarchy oldTaskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(oldTaskHierarchy != null);

            oldTaskHierarchy.setEndExactTimeStamp(now);

            createTaskHierarchy(newParentTask, task, now);
        }

        save(dataId);
    }

    public synchronized void setTaskEndTimeStamp(ArrayList<Integer> dataIds, int taskId) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamp");

        Assert.assertTrue(dataIds != null);
        Assert.assertTrue(!dataIds.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        Assert.assertTrue(task.current(now));

        task.setEndExactTimeStamp(now);

        save(dataIds);
    }

    public synchronized void setTaskEndTimeStamps(int dataId, ArrayList<Integer> taskIds) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps");

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

    public synchronized int createCustomTime(String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.log("DomainFactory.createCustomTime");

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

        return customTime.getId();
    }

    public synchronized void updateCustomTime(int dataId, int customTimeId, String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.log("DomainFactory.updateCustomTime");

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

    public synchronized void setCustomTimeCurrent(int dataId, List<Integer> customTimeIds) {
        MyCrashlytics.log("DomainFactory.setCustomTimeCurrent");

        Assert.assertTrue(customTimeIds != null);
        Assert.assertTrue(!customTimeIds.isEmpty());

        for (int customTimeId : customTimeIds) {
            CustomTime customTime = mCustomTimes.get(customTimeId);
            Assert.assertTrue(customTime != null);

            customTime.setCurrent();
        }

        save(dataId);
    }

    public synchronized void updateTaskOldestVisible() {
        MyCrashlytics.log("DomainFactory.updateTaskOldestVisible");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Irrelevant irrelevant = setIrrelevant(now);
        Assert.assertTrue(irrelevant != null);

        save(0);

        removeIrrelevant(irrelevant);
    }

    Irrelevant setIrrelevant(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        for (Task task : mTasks.values())
            task.updateOldestVisible(now);

        // relevant hack
        List<Task> irrelevantTasks = Stream.of(mTasks.values())
                .filter(task -> !task.isRelevant(now))
                .collect(Collectors.toList());

        List<Instance> irrelevantInstances = Stream.of(mExistingInstances)
                .filter(instance -> !instance.isRelevant(now))
                .collect(Collectors.toList());

        List<Task> relevantTasks = new ArrayList<>(mTasks.values());
        relevantTasks.removeAll(irrelevantTasks);

        List<Instance> relevantInstances = new ArrayList<>(mExistingInstances);
        relevantInstances.removeAll(irrelevantInstances);

        List<CustomTime> irrelevantCustomTimes = Stream.of(mCustomTimes.values())
                .filter(customTime -> !customTime.isRelevant(relevantTasks, relevantInstances, now))
                .collect(Collectors.toList());

        Stream.of(irrelevantTasks)
                .forEach(Task::setRelevant);

        Stream.of(irrelevantInstances)
                .forEach(Instance::setRelevant);

        Stream.of(irrelevantCustomTimes)
                .forEach(CustomTime::setRelevant);

        return new Irrelevant(irrelevantCustomTimes, irrelevantTasks, irrelevantInstances);
    }

    void removeIrrelevant(Irrelevant irrelevant) {
        Assert.assertTrue(irrelevant != null);

        for (Task task : irrelevant.mTasks) {
            mTasks.remove(task.getId());

            List<TaskHierarchy> irrelevantTaskHierarchies = Stream.of(mTaskHierarchies.values())
                    .filter(taskHierarchy -> irrelevant.mTasks.contains(taskHierarchy.getChildTask()))
                    .collect(Collectors.toList());

            for (TaskHierarchy irrelevantTaskHierarchy : irrelevantTaskHierarchies)
                mTaskHierarchies.remove(irrelevantTaskHierarchy.getId());
        }

        Stream.of(irrelevant.mInstances)
                .forEach(mExistingInstances::remove);

        Stream.of(irrelevant.mCustomTimes)
                .forEach(mCustomTimes::remove);
    }

    public synchronized void createRootTask(int dataId, String name) {
        MyCrashlytics.log("DomainFactory.createRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        TaskRecord taskRecord = mPersistenceManager.createTaskRecord(name, now);
        Assert.assertTrue(taskRecord != null);

        Task childTask = new Task(this, taskRecord);
        Assert.assertTrue(!mTasks.containsKey(childTask.getId()));
        mTasks.put(childTask.getId(), childTask);

        save(dataId);
    }

    public synchronized void createJoinRootTask(int dataId, String name, List<Integer> joinTaskIds) {
        MyCrashlytics.log("DomainFactory.createJoinRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        TaskRecord taskRecord = mPersistenceManager.createTaskRecord(name, now);
        Assert.assertTrue(taskRecord != null);

        Task task = new Task(this, taskRecord);
        Assert.assertTrue(!mTasks.containsKey(task.getId()));
        mTasks.put(task.getId(), task);

        joinTasks(task, joinTaskIds, now);

        save(dataId);
    }

    public synchronized void updateRootTask(int dataId, int taskId, String name) {
        MyCrashlytics.log("DomainFactory.updateRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        task.setName(name);

        TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
        if (taskHierarchy != null)
            taskHierarchy.setEndExactTimeStamp(now);

        List<Schedule> schedules = task.getCurrentSchedules(now);
        Assert.assertTrue(schedules != null);

        Stream.of(schedules)
                .forEach(schedule -> schedule.setEndExactTimeStamp(now));

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

    Instance getExistingInstance(Task task, DateTime scheduleDateTime) {
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
            return null;
        }
    }

    Instance getInstance(Task task, DateTime scheduleDateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDateTime != null);

        Instance existingInstance = getExistingInstance(task, scheduleDateTime);

        if (existingInstance != null) {
            return existingInstance;
        } else {
            return new Instance(this, task, scheduleDateTime);
        }
    }

    List<Instance> getPastInstances(Task task, ExactTimeStamp now) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(now != null);

        HashSet<Instance> allInstances = new HashSet<>();

        allInstances.addAll(Stream.of(mExistingInstances)
                .filter(instance -> instance.getTaskId() == task.getId())
                .filter(instance -> instance.getScheduleDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0)
                .collect(Collectors.toList()));

        allInstances.addAll(task.getInstances(null, now.plusOne(), now));

        return new ArrayList<>(allInstances);
    }

    private List<Instance> getRootInstances(ExactTimeStamp startExactTimeStamp, ExactTimeStamp endExactTimeStamp, ExactTimeStamp now) {
        Assert.assertTrue(endExactTimeStamp != null);
        Assert.assertTrue(startExactTimeStamp == null || startExactTimeStamp.compareTo(endExactTimeStamp) < 0);
        Assert.assertTrue(now != null);

        HashSet<Instance> allInstances = new HashSet<>();

        for (Instance instance : mExistingInstances) {
            ExactTimeStamp instanceExactTimeStamp = instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp();

            if (startExactTimeStamp != null && startExactTimeStamp.compareTo(instanceExactTimeStamp) > 0)
                continue;

            if (endExactTimeStamp.compareTo(instanceExactTimeStamp) <= 0)
                continue;

            allInstances.add(instance);
        }

        for (Task task : mTasks.values()) {
            for (Instance instance : task.getInstances(startExactTimeStamp, endExactTimeStamp, now)) {
                ExactTimeStamp instanceExactTimeStamp = instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp();

                if (startExactTimeStamp != null && startExactTimeStamp.compareTo(instanceExactTimeStamp) > 0)
                    continue;

                if (endExactTimeStamp.compareTo(instanceExactTimeStamp) <= 0)
                    continue;

                allInstances.add(instance);
            }
        }

        return Stream.of(allInstances)
                .filter(instance -> instance.isRootInstance(now))
                .filter(instance -> instance.isVisible(now))
                .collect(Collectors.toList());
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

        List<ScheduleRecord> scheduleRecords = mPersistenceManager.getScheduleRecords(task);
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

        SingleScheduleRecord singleScheduleRecord = mPersistenceManager.getSingleScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(singleScheduleRecord != null);

        return new SingleSchedule(scheduleRecord, rootTask, this, singleScheduleRecord);
    }

    private DailySchedule loadDailySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        DailyScheduleRecord dailyScheduleRecord = mPersistenceManager.getDailyScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(dailyScheduleRecord != null);

        return new DailySchedule(scheduleRecord, rootTask, this, dailyScheduleRecord);
    }

    private WeeklySchedule loadWeeklySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        WeeklyScheduleRecord weeklyScheduleRecord = mPersistenceManager.getWeeklyScheduleRecords(scheduleRecord.getId());
        Assert.assertTrue(weeklyScheduleRecord != null);

        return new WeeklySchedule(scheduleRecord, rootTask, this, weeklyScheduleRecord);
    }

    private Task createRootTaskHelper(String name, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(startExactTimeStamp != null);

        TaskRecord taskRecord = mPersistenceManager.createTaskRecord(name, startExactTimeStamp);
        Assert.assertTrue(taskRecord != null);

        Task rootTask = new Task(this, taskRecord);

        Assert.assertTrue(!mTasks.containsKey(rootTask.getId()));
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
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

    private void joinTasks(Task newParentTask, List<Integer> joinTaskIds, ExactTimeStamp now) {
        Assert.assertTrue(newParentTask != null);
        Assert.assertTrue(newParentTask.current(now));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        for (int joinTaskId : joinTaskIds) {
            Task joinTask = mTasks.get(joinTaskId);
            Assert.assertTrue(joinTask != null);
            Assert.assertTrue(joinTask.current(now));

            if (joinTask.isRootTask(now)) {
                List<Schedule> schedules = joinTask.getCurrentSchedules(now);
                Assert.assertTrue(schedules != null);

                Stream.of(schedules)
                        .forEach(schedule -> schedule.setEndExactTimeStamp(now));
            } else {
                TaskHierarchy taskHierarchy = getParentTaskHierarchy(joinTask, now);
                Assert.assertTrue(taskHierarchy != null);

                taskHierarchy.setEndExactTimeStamp(now);
            }

            createTaskHierarchy(newParentTask, joinTask, now);
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

        SingleScheduleRecord singleScheduleDateTimeRecord = mPersistenceManager.createSingleScheduleRecord(scheduleRecord.getId(), date, time);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        return new SingleSchedule(scheduleRecord, rootTask, this, singleScheduleDateTimeRecord);
    }

    private List<Schedule> createSchedules(Task rootTask, List<CreateTaskLoader.ScheduleData> scheduleDatas, ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(scheduleDatas != null);
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(startExactTimeStamp != null);
        Assert.assertTrue(rootTask.current(startExactTimeStamp));

        List<Schedule> schedules = new ArrayList<>();

        for (CreateTaskLoader.ScheduleData scheduleData : scheduleDatas) {
            Assert.assertTrue(scheduleData != null);

            switch (scheduleData.getScheduleType()) {
                case SINGLE: {
                    CreateTaskLoader.SingleScheduleData singleScheduleData = (CreateTaskLoader.SingleScheduleData) scheduleData;

                    Date date = singleScheduleData.Date;
                    Time time = getTime(singleScheduleData.TimePair);

                    Assert.assertTrue(date != null);
                    Assert.assertTrue(time != null);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.SINGLE, startExactTimeStamp);
                    Assert.assertTrue(scheduleRecord != null);

                    SingleScheduleRecord singleScheduleRecord = mPersistenceManager.createSingleScheduleRecord(scheduleRecord.getId(), date, time);
                    Assert.assertTrue(singleScheduleRecord != null);

                    schedules.add(new SingleSchedule(scheduleRecord, rootTask, this, singleScheduleRecord));
                    break;
                }
                case DAILY: {
                    CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                    Time time = getTime(dailyScheduleData.TimePair);
                    Assert.assertTrue(time != null);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.DAILY, startExactTimeStamp);
                    Assert.assertTrue(scheduleRecord != null);

                    DailyScheduleRecord dailyScheduleRecord = mPersistenceManager.createDailyScheduleRecord(scheduleRecord.getId(), time);
                    Assert.assertTrue(dailyScheduleRecord != null);

                    schedules.add(new DailySchedule(scheduleRecord, rootTask, this, dailyScheduleRecord));
                    break;
                }
                case WEEKLY: {
                    CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;

                    DayOfWeek dayOfWeek = weeklyScheduleData.DayOfWeek;
                    Time time = getTime(weeklyScheduleData.TimePair);

                    Assert.assertTrue(dayOfWeek != null);
                    Assert.assertTrue(time != null);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.WEEKLY, startExactTimeStamp);
                    Assert.assertTrue(scheduleRecord != null);

                    WeeklyScheduleRecord weeklyScheduleRecord = mPersistenceManager.createWeeklyScheduleRecord(scheduleRecord.getId(), dayOfWeek, time);
                    Assert.assertTrue(weeklyScheduleRecord != null);

                    schedules.add(new WeeklySchedule(scheduleRecord, rootTask, this, weeklyScheduleRecord));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        return schedules;
    }

    List<Task> getChildTasks(Task parentTask, ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(exactTimeStamp));

        return Stream.of(getChildTaskHierarchies(parentTask))
                .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp))
                .map(TaskHierarchy::getChildTask)
                .filter(childTask -> childTask.current(exactTimeStamp))
                .sortBy(Task::getId)
                .collect(Collectors.toList());
    }

    List<TaskHierarchy> getChildTaskHierarchies(Task parentTask) {
        Assert.assertTrue(parentTask != null);

        return Stream.of(mTaskHierarchies.values())
                .filter(taskHierarchy -> taskHierarchy.getParentTask() == parentTask)
                .collect(Collectors.toList());
    }

    List<TaskHierarchy> getParentTaskHierarchies(Task childTask) {
        Assert.assertTrue(childTask != null);

        return Stream.of(mTaskHierarchies.values())
                .filter(taskHierarchy -> taskHierarchy.getChildTask() == childTask)
                .collect(Collectors.toList());
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

    private List<CustomTime> getCurrentCustomTimes() {
        return Stream.of(mCustomTimes.values())
                .filter(CustomTime::getCurrent)
                .collect(Collectors.toList());
    }

    private HashMap<InstanceKey, GroupListLoader.InstanceData> getChildInstanceDatas(Instance instance, ExactTimeStamp now, WeakReference<GroupListLoader.InstanceDataParent> instanceDataParentReference) {
        Assert.assertTrue(instance != null);
        Assert.assertTrue(now != null);
        Assert.assertTrue(instanceDataParentReference != null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();

        ArrayList<Instance> childInstances = instance.getChildInstances(now);
        for (Instance childInstance : childInstances) {
            Task childTask = mTasks.get(childInstance.getTaskId());
            Assert.assertTrue(childTask != null);

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), instanceDataParentReference, childInstance.getInstanceDateTime().getTime().getTimePair());
            instanceData.setChildren(getChildInstanceDatas(childInstance, now, new WeakReference<>(instanceData)));
            instanceDatas.put(childInstance.getInstanceKey(), instanceData);
        }

        return instanceDatas;
    }

    private List<TaskListLoader.TaskData> getChildTaskDatas(Task parentTask, ExactTimeStamp now, Context context) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(now != null);
        Assert.assertTrue(context != null);

        return Stream.of(parentTask.getChildTasks(now))
                .sortBy(Task::getId)
                .map(childTask -> new TaskListLoader.TaskData(childTask.getId(), childTask.getName(), childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.isRootTask(now)))
                .collect(Collectors.toList());
    }

    private TreeMap<Integer, CreateTaskLoader.TaskTreeData> getChildTaskDatas(ExactTimeStamp now, Task parentTask, Context context, List<Integer> excludedTaskIds) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(context != null);
        Assert.assertTrue(excludedTaskIds != null);

        return Stream.of(parentTask.getChildTasks(now))
                .filterNot(childTask -> excludedTaskIds.contains(childTask.getId()))
                .collect(Collectors.toMap(Task::getId, childTask -> new CreateTaskLoader.TaskTreeData(childTask.getName(), getChildTaskDatas(now, childTask, context, excludedTaskIds), childTask.getId(), childTask.getScheduleText(context, now)), TreeMap::new));
    }

    private TreeMap<Integer, CreateTaskLoader.TaskTreeData> getTaskDatas(Context context, ExactTimeStamp now, List<Integer> excludedTaskIds) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(now != null);
        Assert.assertTrue(excludedTaskIds != null);

        TreeMap<Integer, CreateTaskLoader.TaskTreeData> taskDatas = new TreeMap<>((lhs, rhs) -> -lhs.compareTo(rhs));

        for (Task task : mTasks.values()) {
            if (!task.current(now)) {
                continue;
            }

            if (!task.isVisible(now))
                continue;

            if (!task.isRootTask(now))
                continue;

            if (excludedTaskIds.contains(task.getId()))
                continue;

            taskDatas.put(task.getId(), new CreateTaskLoader.TaskTreeData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskIds), task.getId(), task.getScheduleText(context, now)));
        }

        return taskDatas;
    }

    static class Irrelevant {
        public final List<CustomTime> mCustomTimes;
        public final List<Task> mTasks;
        public final List<Instance> mInstances;

        public Irrelevant(List<CustomTime> customTimes, List<Task> tasks, List<Instance> instances) {
            Assert.assertTrue(customTimes != null);
            Assert.assertTrue(tasks != null);
            Assert.assertTrue(instances != null);

            mCustomTimes = customTimes;
            mTasks = tasks;
            mInstances = instances;
        }
    }
}
