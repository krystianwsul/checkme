package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.loaders.DailyScheduleLoader;
import com.krystianwsul.checkme.loaders.EditInstanceLoader;
import com.krystianwsul.checkme.loaders.EditInstancesLoader;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.loaders.ParentLoader;
import com.krystianwsul.checkme.loaders.SchedulePickerLoader;
import com.krystianwsul.checkme.loaders.ShowCustomTimeLoader;
import com.krystianwsul.checkme.loaders.ShowCustomTimesLoader;
import com.krystianwsul.checkme.loaders.ShowGroupLoader;
import com.krystianwsul.checkme.loaders.ShowInstanceLoader;
import com.krystianwsul.checkme.loaders.ShowTaskLoader;
import com.krystianwsul.checkme.loaders.SingleScheduleLoader;
import com.krystianwsul.checkme.loaders.TaskListLoader;
import com.krystianwsul.checkme.loaders.WeeklyScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.persistencemodel.CustomTimeRecord;
import com.krystianwsul.checkme.persistencemodel.DailyScheduleTimeRecord;
import com.krystianwsul.checkme.persistencemodel.InstanceRecord;
import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.SingleScheduleDateTimeRecord;
import com.krystianwsul.checkme.persistencemodel.TaskHierarchyRecord;
import com.krystianwsul.checkme.persistencemodel.TaskRecord;
import com.krystianwsul.checkme.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;
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

    public long getInstantiateMilis() {
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

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = mTasks.get(instance.getTaskId());
            Assert.assertTrue(task != null);

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), new WeakReference<>(data));
            instanceData.setChildren(getChildInstanceDatas(instance, now, new WeakReference<>(instanceData)));
            instanceDatas.put(instanceData.InstanceKey, instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    public synchronized ShowGroupLoader.Data getShowGroupData(Context context, TimeStamp timeStamp) {
        fakeDelay();

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
        Assert.assertTrue(!currentInstances.isEmpty());

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
        Assert.assertTrue(!currentInstances.isEmpty());

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = mTasks.get(instance.getTaskId());
            Assert.assertTrue(task != null);

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), null, instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), new WeakReference<>(data));
            instanceData.setChildren(getChildInstanceDatas(instance, now, new WeakReference<>(instanceData)));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    public synchronized GroupListLoader.Data getGroupListData(Context context, InstanceKey instanceKey) {
        fakeDelay();

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

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, task.current(now));

        ArrayList<Instance> childInstances = instance.getChildInstances(now);
        for (Instance childInstance : childInstances) {
            Task childTask = mTasks.get(childInstance.getTaskId());
            Assert.assertTrue(childTask != null);

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), new WeakReference<>(data));
            instanceData.setChildren(getChildInstanceDatas(childInstance, now, new WeakReference<>(instanceData)));
            instanceDatas.put(childInstance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    public synchronized GroupListLoader.Data getGroupListData(Context context, ArrayList<InstanceKey> instanceKeys) {
        fakeDelay();

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

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : instances) {
            Task task = mTasks.get(instance.getTaskId());
            Assert.assertTrue(task != null);

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), new WeakReference<>(data));
            instanceData.setChildren(getChildInstanceDatas(instance, now, new WeakReference<>(instanceData)));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    public synchronized ShowInstanceLoader.Data getShowInstanceData(Context context, InstanceKey instanceKey) {
        fakeDelay();

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

    public synchronized CreateTaskLoader.Data getCreateChildTaskData(Integer taskId, Context context) {
        fakeDelay();

        Assert.assertTrue(context != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        CreateTaskLoader.TaskData taskData = null;
        if (taskId != null) {
            Task task = mTasks.get(taskId);
            Assert.assertTrue(task != null);

            Integer parentTaskId;
            ScheduleType scheduleType;

            if (task.isRootTask(now)) {
                Schedule schedule = task.getCurrentSchedule(now);

                parentTaskId = null;
                scheduleType = (schedule == null ? null : schedule.getType());
            } else {
                Task parentTask = task.getParentTask(now);
                Assert.assertTrue(parentTask != null);

                parentTaskId = parentTask.getId();
                scheduleType = null;
            }

            taskData = new CreateTaskLoader.TaskData(task.getName(), parentTaskId, scheduleType);
        }

        return new CreateTaskLoader.Data(taskData);
    }

    public synchronized ParentLoader.Data getParentData(Integer childTaskId, Context context, List<Integer> excludedTaskIds) {
        fakeDelay();

        Assert.assertTrue(context != null);
        Assert.assertTrue(excludedTaskIds != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        TreeMap<Integer, ParentLoader.TaskData> taskDatas = getTaskDatas(context, now, excludedTaskIds);
        Assert.assertTrue(taskDatas != null);

        ParentLoader.ChildTaskData childTaskData = null;
        if (childTaskId != null) {
            Task childTask = mTasks.get(childTaskId);
            Assert.assertTrue(childTask != null);

            Task parentTask = childTask.getParentTask(now);
            if (parentTask != null)
                childTaskData = new ParentLoader.ChildTaskData(childTask.getName(), parentTask.getId());
        }

        return new ParentLoader.Data(taskDatas, childTaskData);
    }

    public synchronized SchedulePickerLoader.Data getSchedulePickerData(Context context, Integer taskId) {
        fakeDelay();

        Assert.assertTrue(context != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        SchedulePickerLoader.RootTaskData rootTaskData = null;
        if (taskId != null) {
            Task task = mTasks.get(taskId);
            Assert.assertTrue(task != null);

            if (task.isRootTask(now)) {
                Schedule schedule = task.getCurrentSchedule(now);
                if (schedule != null)
                    rootTaskData = new SchedulePickerLoader.RootTaskData(task.getCurrentSchedule(now).getType());
            }
        }

        return new SchedulePickerLoader.Data(rootTaskData);
    }

    public synchronized SingleScheduleLoader.Data getSingleScheduleData(Integer rootTaskId) {
        fakeDelay();

        SingleScheduleLoader.ScheduleData scheduleData = null;

        Map<Integer, CustomTime> customTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getId, customTime -> customTime));

        if (rootTaskId != null) {
            Task rootTask = mTasks.get(rootTaskId);
            Assert.assertTrue(rootTask != null);

            ExactTimeStamp now = ExactTimeStamp.getNow();

            SingleSchedule singleSchedule = (SingleSchedule) rootTask.getCurrentSchedule(now);
            Assert.assertTrue(singleSchedule != null);
            Assert.assertTrue(singleSchedule.current(now));

            Instance instance = singleSchedule.getInstance(rootTask);

            scheduleData = new SingleScheduleLoader.ScheduleData(instance.getInstanceDate(), instance.getInstanceTimePair());

            CustomTime customTime = singleSchedule.getTime().getPair().first;
            if (customTime != null)
                customTimes.put(customTime.getId(), customTime);
        }

        HashMap<Integer, SingleScheduleLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes.values())
            customTimeDatas.put(customTime.getId(), new SingleScheduleLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new SingleScheduleLoader.Data(scheduleData, customTimeDatas);
    }

    public synchronized DailyScheduleLoader.Data getDailyScheduleData(Integer rootTaskId) {
        fakeDelay();

        ArrayList<DailyScheduleLoader.ScheduleData> scheduleDatas = null;

        Map<Integer, CustomTime> customTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getId, customTime -> customTime));

        if (rootTaskId != null) {
            Task rootTask = mTasks.get(rootTaskId);
            Assert.assertTrue(rootTask != null);

            ExactTimeStamp now = ExactTimeStamp.getNow();

            DailySchedule dailySchedule = (DailySchedule) rootTask.getCurrentSchedule(now);
            Assert.assertTrue(dailySchedule != null);
            Assert.assertTrue(dailySchedule.current(now));

            List<Time> times = dailySchedule.getTimes();
            scheduleDatas = new ArrayList<>();
            for (Time time : times) {
                scheduleDatas.add(new DailyScheduleLoader.ScheduleData(time.getTimePair()));

                CustomTime customTime = time.getPair().first;
                if (customTime != null)
                    customTimes.put(customTime.getId(), customTime);
            }
        }

        HashMap<Integer, DailyScheduleLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes.values())
            customTimeDatas.put(customTime.getId(), new DailyScheduleLoader.CustomTimeData(customTime.getId(), customTime.getName()));

        return new DailyScheduleLoader.Data(scheduleDatas, customTimeDatas);
    }

    public synchronized WeeklyScheduleLoader.Data getWeeklyScheduleData(Integer rootTaskId) {
        fakeDelay();

        ArrayList<WeeklyScheduleLoader.ScheduleData> scheduleDatas = null;

        Map<Integer, CustomTime> customTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getId, customTime -> customTime));

        if (rootTaskId != null) {
            Task rootTask = mTasks.get(rootTaskId);
            Assert.assertTrue(rootTask != null);

            ExactTimeStamp now = ExactTimeStamp.getNow();

            WeeklySchedule weeklySchedule = (WeeklySchedule) rootTask.getCurrentSchedule(now);
            Assert.assertTrue(weeklySchedule != null);
            Assert.assertTrue(weeklySchedule.current(now));

            List<Pair<DayOfWeek, Time>> pairs = weeklySchedule.getDayOfWeekTimes();
            scheduleDatas = new ArrayList<>();
            for (Pair<DayOfWeek, Time> pair : pairs) {
                scheduleDatas.add(new WeeklyScheduleLoader.ScheduleData(pair.first, pair.second.getTimePair()));

                CustomTime customTime = pair.second.getPair().first;
                if (customTime != null)
                    customTimes.put(customTime.getId(), customTime);
            }
        }

        HashMap<Integer, WeeklyScheduleLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes.values())
            customTimeDatas.put(customTime.getId(), new WeeklyScheduleLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new WeeklyScheduleLoader.Data(scheduleDatas, customTimeDatas);
    }

    public synchronized ShowTaskLoader.Data getShowTaskData(int taskId, Context context) {
        fakeDelay();

        Assert.assertTrue(context != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);
        Assert.assertTrue(task.current(now));

        return new ShowTaskLoader.Data(task.isRootTask(now), task.getName(), task.getScheduleText(context, now), task.getId());
    }

    public synchronized TaskListLoader.Data getTaskListData(Context context, Integer taskId) {
        fakeDelay();

        Assert.assertTrue(context != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

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
                Schedule schedule = task.getCurrentSchedule(now);
                if (schedule != null) {
                    TimeStamp scheduleTimeStamp = schedule.getNextAlarm(now);
                    if (scheduleTimeStamp != null) {
                        Assert.assertTrue(scheduleTimeStamp.toExactTimeStamp().compareTo(now) > 0);
                        if (nextAlarm == null || scheduleTimeStamp.compareTo(nextAlarm) < 0)
                            nextAlarm = scheduleTimeStamp;
                    }
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

    public synchronized void setInstancesDateTime(int dataId, Set<InstanceKey> instanceKeys, Date instanceDate, TimePair instanceTimePair) {
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

    public synchronized void updateInstancesShown(int dataId, List<InstanceKey> showInstanceKeys, List<InstanceKey> hideInstanceKeys) {
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

        Task rootTask = createRootTaskHelper(name, now);
        Assert.assertTrue(rootTask != null);

        Time time = getTime(timePair);

        Schedule schedule = createSingleSchedule(rootTask, date, time, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public synchronized void createDailyScheduleRootTask(int dataId, String name, List<TimePair> timePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTaskHelper(name, now);
        Assert.assertTrue(rootTask != null);

        List<Time> times = getTimes(timePairs);

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

        Task rootTask = createRootTaskHelper(name, now);
        Assert.assertTrue(rootTask != null);

        Schedule schedule = createWeeklySchedule(rootTask, dayOfWeekTimePairs, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        save(dataId);
    }

    public synchronized void updateSingleScheduleTask(int dataId, int taskId, String name, Date date, TimePair timePair) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue(timePair != null);

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(task.current(now));

        task.setName(name);

        if (task.isRootTask(now) && task.getCurrentSchedule(now) != null && task.getCurrentSchedule(now).getType() == ScheduleType.SINGLE) {
            SingleSchedule singleSchedule = (SingleSchedule) task.getCurrentSchedule(now);

            Instance instance = singleSchedule.getInstance(task);
            Assert.assertTrue(instance != null);

            instance.setInstanceDateTime(date, timePair, now);
        } else {
            if (task.isRootTask(now)) {
                Schedule schedule = task.getCurrentSchedule(now);
                if (schedule != null)
                    schedule.setEndExactTimeStamp(now);
            } else {
                TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
                Assert.assertTrue(taskHierarchy != null);

                taskHierarchy.setEndExactTimeStamp(now);
            }

            Schedule schedule = createSingleSchedule(task, date, getTime(timePair), now);
            Assert.assertTrue(schedule != null);

            task.addSchedule(schedule);
        }

        save(dataId);
    }

    public synchronized void updateDailyScheduleTask(int dataId, int taskId, String name, List<TimePair> timePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(task.current(now));

        task.setName(name);

        if (task.isRootTask(now)) {
            Schedule schedule = task.getCurrentSchedule(now);
            if (schedule != null) {
                Assert.assertTrue(schedule.current(now));

                schedule.setEndExactTimeStamp(now);
            }
        } else {
            TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(taskHierarchy != null);

            taskHierarchy.setEndExactTimeStamp(now);
        }

        List<Time> times = getTimes(timePairs);

        Schedule schedule = createDailySchedule(task, times, now);
        Assert.assertTrue(schedule != null);

        task.addSchedule(schedule);

        save(dataId);
    }

    public synchronized void updateWeeklyScheduleTask(int dataId, int taskId, String name, ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(task.current(now));

        task.setName(name);

        if (task.isRootTask(now)) {
            Schedule schedule = task.getCurrentSchedule(now);
            if (schedule != null) {
                Assert.assertTrue(schedule.current(now));

                schedule.setEndExactTimeStamp(now);
            }
        } else {
            TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(taskHierarchy != null);

            taskHierarchy.setEndExactTimeStamp(now);
        }

        Schedule schedule = createWeeklySchedule(task, dayOfWeekTimePairs, now);
        Assert.assertTrue(schedule != null);

        task.addSchedule(schedule);

        save(dataId);
    }

    public synchronized void createSingleScheduleJoinRootTask(int dataId, String name, Date date, TimePair timePair, List<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(date != null);
        Assert.assertTrue(timePair != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTaskHelper(name, now);
        Assert.assertTrue(rootTask != null);

        Time time = getTime(timePair);
        Assert.assertTrue(time != null);

        Schedule schedule = createSingleSchedule(rootTask, date, time, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        joinTasks(rootTask, joinTaskIds, now);

        save(dataId);
    }

    public synchronized void createDailyScheduleJoinRootTask(int dataId, String name, List<TimePair> timePairs, List<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTaskHelper(name, now);
        Assert.assertTrue(rootTask != null);

        List<Time> times = getTimes(timePairs);

        Schedule schedule = createDailySchedule(rootTask, times, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        joinTasks(rootTask, joinTaskIds, now);

        save(dataId);
    }

    public synchronized void createWeeklyScheduleJoinRootTask(int dataId, String name, ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs, List<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(dayOfWeekTimePairs != null);
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task rootTask = createRootTaskHelper(name, now);
        Assert.assertTrue(rootTask != null);

        Schedule schedule = createWeeklySchedule(rootTask, dayOfWeekTimePairs, now);
        Assert.assertTrue(schedule != null);

        rootTask.addSchedule(schedule);

        joinTasks(rootTask, joinTaskIds, now);

        save(dataId);
    }

    public synchronized void createChildTask(int dataId, int parentTaskId, String name) {
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

        save(dataId);
    }

    public synchronized void createJoinChildTask(int dataId, int parentTaskId, String name, List<Integer> joinTaskIds) {
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
        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        task.setName(name);

        Task newParentTask = mTasks.get(parentTaskId);
        Assert.assertTrue(newParentTask != null);

        Task oldParentTask = task.getParentTask(now);
        if (oldParentTask == null) {
            Schedule schedule = task.getCurrentSchedule(now);
            if (schedule != null) {
                Assert.assertTrue(schedule.current(now));

                schedule.setEndExactTimeStamp(now);
            }

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
        ExactTimeStamp now = ExactTimeStamp.getNow();

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

        for (Task task : mTasks.values())
            task.updateOldestVisible(now);

        save(0);

        for (Task task : irrelevantTasks) {
            mTasks.remove(task.getId());

            List<TaskHierarchy> irrelevantTaskHierarchies = Stream.of(mTaskHierarchies.values())
                    .filter(taskHierarchy -> irrelevantTasks.contains(taskHierarchy.getChildTask()))
                    .collect(Collectors.toList());

            for (TaskHierarchy irrelevanTaskHierarchy : irrelevantTaskHierarchies)
                mTaskHierarchies.remove(irrelevanTaskHierarchy.getId());
        }

        Stream.of(irrelevantInstances)
                .forEach(mExistingInstances::remove);

        Stream.of(irrelevantCustomTimes)
                .forEach(mCustomTimes::remove);
    }

    public synchronized void createRootTask(int dataId, String name) {
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
        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = mTasks.get(taskId);
        Assert.assertTrue(task != null);

        task.setName(name);

        TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
        if (taskHierarchy != null)
            taskHierarchy.setEndExactTimeStamp(now);

        Schedule schedule = task.getCurrentSchedule(now);
        if (schedule != null)
            schedule.setEndExactTimeStamp(now);

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

        List<DailyScheduleTimeRecord> dailyScheduleTimeRecords = mPersistenceManager.getDailyScheduleTimeRecords(dailySchedule);
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

        List<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = mPersistenceManager.getWeeklyScheduleDayOfWeekTimeRecords(weeklySchedule);
        Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecords != null);
        Assert.assertTrue(!weeklyScheduleDayOfWeekTimeRecords.isEmpty());

        for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : weeklyScheduleDayOfWeekTimeRecords) {
            WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime = new WeeklyScheduleDayOfWeekTime(this, weeklyScheduleDayOfWeekTimeRecord);
            weeklySchedule.addWeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTime);
        }

        return weeklySchedule;
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

    private List<Time> getTimes(List<TimePair> timePairs) {
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        return Stream.of(timePairs)
                .map(this::getTime)
                .collect(Collectors.toList());
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
                Schedule schedule = joinTask.getCurrentSchedule(now);
                if (schedule != null) {
                    Assert.assertTrue(schedule.current(now));

                    schedule.setEndExactTimeStamp(now);
                }
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

        SingleSchedule singleSchedule = new SingleSchedule(scheduleRecord, rootTask, this);

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = mPersistenceManager.createSingleScheduleDateTimeRecord(singleSchedule, date, time);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        singleSchedule.setSingleScheduleDateTime(new SingleScheduleDateTime(this, singleScheduleDateTimeRecord));

        return singleSchedule;
    }

    private DailySchedule createDailySchedule(Task rootTask, List<Time> times, ExactTimeStamp startExactTimeStamp) {
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

    List<Task> getChildTasks(Task parentTask, ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(exactTimeStamp));

        return Stream.of(getTaskHierarchies(parentTask))
                .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp))
                .map(TaskHierarchy::getChildTask)
                .filter(childTask -> childTask.current(exactTimeStamp))
                .sortBy(Task::getId)
                .collect(Collectors.toList());
    }

    List<TaskHierarchy> getTaskHierarchies(Task parentTask) {
        Assert.assertTrue(parentTask != null);

        return Stream.of(mTaskHierarchies.values())
                .filter(taskHierarchy -> taskHierarchy.getParentTask() == parentTask)
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

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), instanceDataParentReference);
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

    private TreeMap<Integer, ParentLoader.TaskData> getChildTaskDatas(ExactTimeStamp now, Task parentTask, Context context, List<Integer> excludedTaskIds) {
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(context != null);
        Assert.assertTrue(excludedTaskIds != null);

        return Stream.of(parentTask.getChildTasks(now))
                .filterNot(childTask -> excludedTaskIds.contains(childTask.getId()))
                .collect(Collectors.toMap(Task::getId, childTask -> new ParentLoader.TaskData(childTask.getName(), getChildTaskDatas(now, childTask, context, excludedTaskIds), childTask.getId(), childTask.getScheduleText(context, now)), TreeMap::new));
    }

    private TreeMap<Integer, ParentLoader.TaskData> getTaskDatas(Context context, ExactTimeStamp now, List<Integer> excludedTaskIds) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(now != null);
        Assert.assertTrue(excludedTaskIds != null);

        TreeMap<Integer, ParentLoader.TaskData> taskDatas = new TreeMap<>((lhs, rhs) -> -lhs.compareTo(rhs));

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

            taskDatas.put(task.getId(), new ParentLoader.TaskData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskIds), task.getId(), task.getScheduleText(context, now)));
        }

        return taskDatas;
    }
}
