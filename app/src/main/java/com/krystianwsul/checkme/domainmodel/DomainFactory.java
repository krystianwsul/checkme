package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteFactory;
import com.krystianwsul.checkme.firebase.RemoteInstance;
import com.krystianwsul.checkme.firebase.RemoteTask;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity;
import com.krystianwsul.checkme.gui.instances.ShowNotificationGroupActivity;
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
import com.krystianwsul.checkme.notifications.GroupNotificationDeleteService;
import com.krystianwsul.checkme.notifications.InstanceDoneService;
import com.krystianwsul.checkme.notifications.InstanceHourService;
import com.krystianwsul.checkme.notifications.InstanceNotificationDeleteService;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.persistencemodel.CustomTimeRecord;
import com.krystianwsul.checkme.persistencemodel.DailyScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.InstanceRecord;
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord;
import com.krystianwsul.checkme.persistencemodel.MonthlyDayScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.MonthlyWeekScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.SingleScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.TaskHierarchyRecord;
import com.krystianwsul.checkme.persistencemodel.TaskRecord;
import com.krystianwsul.checkme.persistencemodel.WeeklyScheduleRecord;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@SuppressLint("UseSparseArrays")
public class DomainFactory {
    private static DomainFactory sDomainFactory;

    private final PersistenceManger mPersistenceManager;

    private final HashMap<Integer, CustomTime> mCustomTimes = new HashMap<>();
    private final HashMap<Integer, Task> mTasks = new HashMap<>();
    private final HashMap<Integer, LocalTaskHierarchy> mTaskHierarchies = new HashMap<>();
    private final ArrayList<Instance> mExistingInstances = new ArrayList<>();

    private static ExactTimeStamp sStart;
    private static ExactTimeStamp sRead;
    private static ExactTimeStamp sStop;

    @Nullable
    private UserData mUserData;

    @Nullable
    private Query mQuery;

    @NonNull
    private final ValueEventListener mValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Log.e("asdf", "DomainFactory.mValueEventListeneronDataChange, dataSnapshot: " + dataSnapshot);

            setRemoteTaskRecords(dataSnapshot);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e("asdf", "DomainFactory.mValueEventListener.onCancelled", databaseError.toException());

            MyCrashlytics.logException(databaseError.toException());
        }
    };

    private RemoteFactory mRemoteFactory;

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

        mRemoteFactory = new RemoteFactory(this, new ArrayList<>());
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

            LocalTaskHierarchy localTaskHierarchy = new LocalTaskHierarchy(this, taskHierarchyRecord);

            Assert.assertTrue(!mTaskHierarchies.containsKey(localTaskHierarchy.getId()));
            mTaskHierarchies.put(localTaskHierarchy.getId(), localTaskHierarchy);
        }

        Collection<InstanceRecord> instanceRecords = mPersistenceManager.getInstanceRecords();
        Assert.assertTrue(instanceRecords != null);

        for (InstanceRecord instanceRecord : instanceRecords) {
            Task task = mTasks.get(instanceRecord.getTaskId());
            Assert.assertTrue(task != null);

            Instance instance = new Instance(this, instanceRecord);
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
        return getTasks().size();
    }

    public int getInstanceCount() {
        return mExistingInstances.size();
    }

    public int getCustomTimeCount() {
        return mCustomTimes.size();
    }

    private void save(@NonNull Context context, int dataId) {
        ArrayList<Integer> dataIds = new ArrayList<>();
        dataIds.add(dataId);
        save(context, dataIds);
    }

    private void save(@NonNull Context context, @NonNull ArrayList<Integer> dataIds) {
        mPersistenceManager.save(context);
        mRemoteFactory.save();
        ObserverHolder.getObserverHolder().notifyDomainObservers(dataIds);
    }

    // firebase

    public synchronized void setUserData(@NonNull UserData userData) {
        if (mUserData != null) {
            Assert.assertTrue(mQuery != null);

            if (mUserData.equals(userData))
                return;

            clearUserData();
        }

        Assert.assertTrue(mUserData == null);
        Assert.assertTrue(mQuery == null);

        mUserData = userData;

        mQuery = DatabaseWrapper.getTaskRecordsQuery(userData);
        mQuery.addValueEventListener(mValueEventListener);
    }

    public synchronized void clearUserData() {
        if (mUserData == null) {
            Assert.assertTrue(mQuery == null);
        } else {
            Assert.assertTrue(mQuery != null);

            mQuery.removeEventListener(mValueEventListener);

            mUserData = null;
            mQuery = null;
        }
    }

    private synchronized void setRemoteTaskRecords(@NonNull DataSnapshot dataSnapshot) {
        mRemoteFactory = new RemoteFactory(this, dataSnapshot.getChildren());

        ObserverHolder.getObserverHolder().notifyDomainObservers(new ArrayList<>());
    }

    @NonNull
    private List<TaskHierarchy> getTaskHierarchies() {
        List<TaskHierarchy> taskHierarchies = new ArrayList<>(mTaskHierarchies.values());
        taskHierarchies.addAll(mRemoteFactory.mRemoteTaskHierarchies.values());
        return taskHierarchies;
    }

    @Nullable
    public TaskHierarchy getParentTaskHierarchy(@NonNull MergedTask childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(childTask.current(exactTimeStamp));

        TaskKey childTaskKey = childTask.getTaskKey();

        List<TaskHierarchy> taskHierarchies = Stream.of(getTaskHierarchies())
                .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp))
                .filter(taskHierarchy -> taskHierarchy.getChildTaskKey().equals(childTaskKey))
                .collect(Collectors.toList());

        if (taskHierarchies.isEmpty()) {
            return null;
        } else {
            Assert.assertTrue(taskHierarchies.size() == 1);
            return taskHierarchies.get(0);
        }
    }

    @NonNull
    private Map<TaskKey, MergedTask> getTasks() {
        return Stream.concat(Stream.of(mTasks.values()), Stream.of(mRemoteFactory.mRemoteTasks.values()))
                .collect(Collectors.toMap(MergedTask::getTaskKey, task -> task));
    }

    @NonNull
    public MergedTask getTask(@NonNull TaskKey taskKey) {
        Map<TaskKey, MergedTask> tasks = getTasks();
        Assert.assertTrue(tasks.containsKey(taskKey));

        MergedTask task = tasks.get(taskKey);
        Assert.assertTrue(task != null);

        return task;
    }

    @NonNull
    public List<MergedTask> getChildTasks(@NonNull MergedTask parentTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(parentTask.current(exactTimeStamp));

        return Stream.of(getChildTaskHierarchies(parentTask))
                .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp))
                .map(TaskHierarchy::getChildTask)
                .filter(childTask -> childTask.current(exactTimeStamp))
                .sortBy(MergedTask::getStartExactTimeStamp)
                .collect(Collectors.toList());
    }

    @NonNull
    public List<TaskHierarchy> getChildTaskHierarchies(@NonNull MergedTask parentTask) {
        TaskKey parentTaskKey = parentTask.getTaskKey();

        return Stream.of(getTaskHierarchies())
                .filter(taskHierarchy -> taskHierarchy.getParentTaskKey().equals(parentTaskKey))
                .collect(Collectors.toList());
    }

    @NonNull
    private List<TaskListLoader.ChildTaskData> getChildTaskDatas(@NonNull MergedTask parentTask, @NonNull ExactTimeStamp now, @NonNull Context context) {
        return Stream.of(parentTask.getChildTasks(now))
                .sortBy(MergedTask::getStartExactTimeStamp)
                .map(childTask -> new TaskListLoader.ChildTaskData(childTask.getName(), childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey()))
                .collect(Collectors.toList());
    }

    @NonNull
    public RemoteFactory getRemoteFactory() {
        return mRemoteFactory;
    }

    @NonNull
    public Map<InstanceKey, MergedInstance> getExistingInstances() {
        return Stream.concat(Stream.of(mExistingInstances), Stream.of(mRemoteFactory.mExistingRemoteInstances.values()))
                .collect(Collectors.toMap(MergedInstance::getInstanceKey, instance -> instance));
    }

    @Nullable
    public InstanceShownRecord getInstanceShownRecord(@NonNull String taskId, int scheduleYear, int scheduleMonth, int scheduleDay, @Nullable Integer scheduleCustomTimeId, @Nullable Integer scheduleHour, @Nullable Integer scheduleMinute) {
        List<InstanceShownRecord> matches;
        if (scheduleCustomTimeId != null) {
            Assert.assertTrue(scheduleHour == null);
            Assert.assertTrue(scheduleMinute == null);

            matches = Stream.of(mPersistenceManager.getInstancesShownRecords())
                    .filter(instanceShownRecord -> instanceShownRecord.getTaskId().equals(taskId))
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleYear() == scheduleYear)
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleMonth() == scheduleMonth)
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleDay() == scheduleDay)
                    .filter(instanceShownRecord -> scheduleCustomTimeId.equals(instanceShownRecord.getScheduleCustomTimeId()))
                    .collect(Collectors.toList());
        } else {
            Assert.assertTrue(scheduleHour != null);
            Assert.assertTrue(scheduleMinute != null);

            matches = Stream.of(mPersistenceManager.getInstancesShownRecords())
                    .filter(instanceShownRecord -> instanceShownRecord.getTaskId().equals(taskId))
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleYear() == scheduleYear)
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleMonth() == scheduleMonth)
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleDay() == scheduleDay)
                    .filter(instanceShownRecord -> scheduleHour.equals(instanceShownRecord.getScheduleHour()))
                    .filter(instanceShownRecord -> scheduleMinute.equals(instanceShownRecord.getScheduleMinute()))
                    .collect(Collectors.toList());
        }

        if (matches.isEmpty()) {
            return null;
        } else {
            Assert.assertTrue(matches.size() == 1);

            return matches.get(0);
        }
    }

    @NonNull
    public InstanceShownRecord createInstanceShownRecord(@NonNull RemoteTask remoteTask, @NonNull DateTime scheduleDateTime) {
        return mPersistenceManager.createInstanceShownRecord(remoteTask, scheduleDateTime);
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

    @NonNull
    public synchronized EditInstanceLoader.Data getEditInstanceData(@NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getEditInstanceData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<Integer, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getId, customTime -> customTime));

        MergedInstance instance = getInstance(instanceKey);
        Assert.assertTrue(instance.isRootInstance(now));
        Assert.assertTrue(instance.getDone() == null);

        if (instance.getInstanceTimePair().mCustomTimeId != null) {
            CustomTime customTime = mCustomTimes.get(instance.getInstanceTimePair().mCustomTimeId);
            Assert.assertTrue(customTime != null);

            currentCustomTimes.put(customTime.getId(), customTime);
        }

        TreeMap<Integer, EditInstanceLoader.CustomTimeData> customTimeDatas = new TreeMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getId(), new EditInstanceLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstanceLoader.Data(instance.getInstanceKey(), instance.getInstanceDate(), instance.getInstanceTimePair(), instance.getName(), customTimeDatas);
    }

    @NonNull
    public synchronized EditInstancesLoader.Data getEditInstancesData(@NonNull ArrayList<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getEditInstancesData");

        Assert.assertTrue(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<Integer, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getId, customTime -> customTime));

        HashMap<InstanceKey, EditInstancesLoader.InstanceData> instanceDatas = new HashMap<>();

        for (InstanceKey instanceKey : instanceKeys) {
            MergedInstance instance = getInstance(instanceKey);
            Assert.assertTrue(instance.isRootInstance(now));
            Assert.assertTrue(instance.getDone() == null);

            instanceDatas.put(instanceKey, new EditInstancesLoader.InstanceData(instance.getInstanceDate(), instance.getName()));

            if (instance.getInstanceTimePair().mCustomTimeId != null) {
                CustomTime customTime = mCustomTimes.get(instance.getInstanceTimePair().mCustomTimeId);
                Assert.assertTrue(customTime != null);

                currentCustomTimes.put(customTime.getId(), customTime);
            }
        }

        TreeMap<Integer, EditInstancesLoader.CustomTimeData> customTimeDatas = new TreeMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getId(), new EditInstancesLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstancesLoader.Data(instanceDatas, customTimeDatas);
    }

    @NonNull
    public synchronized ShowCustomTimeLoader.Data getShowCustomTimeData(int customTimeId) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowCustomTimeData");

        CustomTime customTime = mCustomTimes.get(customTimeId);
        Assert.assertTrue(customTime != null);

        HashMap<DayOfWeek, HourMinute> hourMinutes = new HashMap<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values())
            hourMinutes.put(dayOfWeek, customTime.getHourMinute(dayOfWeek));

        return new ShowCustomTimeLoader.Data(customTime.getId(), customTime.getName(), hourMinutes);
    }

    @NonNull
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

    @NonNull
    public synchronized GroupListLoader.Data getGroupListData(@NonNull Context context, int position, @NonNull MainActivity.TimeRange timeRange) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Assert.assertTrue(position >= 0);

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

            startExactTimeStamp = new ExactTimeStamp(new Date(startCalendar), new HourMilli(0, 0, 0, 0));
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

        endExactTimeStamp = new ExactTimeStamp(new Date(endCalendar), new HourMilli(0, 0, 0, 0));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<MergedInstance> currentInstances = getRootInstances(startExactTimeStamp, endExactTimeStamp, now);

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        List<GroupListLoader.TaskData> taskDatas = null;
        if (position == 0) {
            taskDatas = Stream.of(getTasks().values())
                    .filter(task -> task.current(now))
                    .filter(task -> task.isVisible(now))
                    .filter(task -> task.isRootTask(now))
                    .filter(task -> task.getCurrentSchedules(now).isEmpty())
                    .map(task -> new GroupListLoader.TaskData(task.getTaskKey(), task.getName(), getChildTaskDatas(task, now), task.getStartExactTimeStamp()))
                    .collect(Collectors.toList());
        }

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null, taskDatas, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (MergedInstance instance : currentInstances) {
            MergedTask task = getTask(instance.getTaskKey());

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), data, instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), task.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(instance, now, instanceData));
            instanceDatas.put(instanceData.InstanceKey, instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    @NonNull
    private List<GroupListLoader.TaskData> getChildTaskDatas(@NonNull MergedTask parentTask, @NonNull ExactTimeStamp now) {
        return Stream.of(parentTask.getChildTasks(now))
                .map(childTask -> new GroupListLoader.TaskData(childTask.getTaskKey(), childTask.getName(), getChildTaskDatas(childTask, now), childTask.getStartExactTimeStamp()))
                .collect(Collectors.toList());
    }

    public synchronized ShowGroupLoader.Data getShowGroupData(@NonNull Context context, @NonNull TimeStamp timeStamp) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowGroupData");

        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<MergedInstance> rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now);

        List<MergedInstance> currentInstances = Stream.of(rootInstances)
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

    @NonNull
    public synchronized GroupListLoader.Data getGroupListData(@NonNull TimeStamp timeStamp) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<MergedInstance> rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now);

        List<MergedInstance> currentInstances = Stream.of(rootInstances)
                .filter(instance -> instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                .collect(Collectors.toList());

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null, null, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (MergedInstance instance : currentInstances) {
            MergedTask task = getTask(instance.getTaskKey());

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), null, instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), data, instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), task.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(instance, now, instanceData));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    @NonNull
    public synchronized GroupListLoader.Data getGroupListData(@NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        if (getTasks().containsKey(instanceKey.mTaskKey)) {
            MergedTask task = getTask(instanceKey.mTaskKey);
            MergedInstance instance = getInstance(instanceKey);

            GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, task.current(now), null, task.getNote());

            for (MergedInstance childInstance : instance.getChildInstances(now)) {
                MergedTask childTask = getTask(childInstance.getTaskKey());

                Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

                GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), data, childInstance.getInstanceDateTime().getTime().getTimePair(), childTask.getNote(), childTask.getStartExactTimeStamp());
                instanceData.setChildren(getChildInstanceDatas(childInstance, now, instanceData));
                instanceDatas.put(childInstance.getInstanceKey(), instanceData);
            }

            data.setInstanceDatas(instanceDatas);

            return data;
        } else { // todo probably should wrap all data in this loader in a nullable object at some point
            GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, false, null, null);

            data.setInstanceDatas(new HashMap<>());

            return data;
        }
    }

    @NonNull
    public synchronized GroupListLoader.Data getGroupListData(@NonNull Context context, @NonNull ArrayList<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        ArrayList<MergedInstance> instances = new ArrayList<>();
        for (InstanceKey instanceKey : instanceKeys) {
            MergedInstance instance = getInstance(instanceKey);

            if (instance.isRootInstance(now))
                instances.add(instance);
        }

        Collections.sort(instances, (lhs, rhs) -> lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime()));

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null, null, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (MergedInstance instance : instances) {
            MergedTask task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), data, instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), task.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(instance, now, instanceData));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    @NonNull
    public synchronized ShowInstanceLoader.Data getShowInstanceData(@NonNull Context context, @NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowInstanceData");

        if (!getTasks().containsKey(instanceKey.mTaskKey))
            return new ShowInstanceLoader.Data(null);

        MergedTask task = getTask(instanceKey.mTaskKey);
        MergedInstance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);
        return new ShowInstanceLoader.Data(new ShowInstanceLoader.InstanceData(instance.getInstanceKey(), instance.getName(), instance.getDisplayText(context, now), instance.getDone() != null, task.current(now), instance.isRootInstance(now), isRootTask));
    }

    @NonNull
    public synchronized CreateTaskLoader.Data getCreateChildTaskData(@Nullable TaskKey taskKey, @NonNull Context context, @NonNull List<TaskKey> excludedTaskKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getCreateChildTaskData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<Integer, CustomTime> customTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getId, customTime -> customTime));

        CreateTaskLoader.TaskData taskData = null;
        if (taskKey != null) {
            MergedTask task = getTasks().get(taskKey);
            Assert.assertTrue(task != null);

            TaskKey parentTaskKey;
            List<CreateTaskLoader.ScheduleData> scheduleDatas = null;

            if (task.isRootTask(now)) {
                List<? extends MergedSchedule> schedules = task.getCurrentSchedules(now);

                parentTaskKey = null;

                if (!schedules.isEmpty()) {
                    scheduleDatas = new ArrayList<>();

                    for (MergedSchedule schedule : schedules) {
                        Assert.assertTrue(schedule != null);
                        Assert.assertTrue(schedule.current(now));

                        switch (schedule.getType()) { // todo firebase
                            case SINGLE: {
                                SingleSchedule singleSchedule = (SingleSchedule) schedule;

                                scheduleDatas.add(new CreateTaskLoader.SingleScheduleData(singleSchedule.getDate(), singleSchedule.getTime().getTimePair()));

                                CustomTime weeklyCustomTime = singleSchedule.getTime().getPair().first;
                                if (weeklyCustomTime != null)
                                    customTimes.put(weeklyCustomTime.getId(), weeklyCustomTime);
                                break;
                            }
                            case DAILY: {
                                DailySchedule dailySchedule = (DailySchedule) schedule;

                                Time time = dailySchedule.getTime();

                                scheduleDatas.add(new CreateTaskLoader.DailyScheduleData(time.getTimePair()));

                                CustomTime dailyCustomTime = time.getPair().first;
                                if (dailyCustomTime != null)
                                    customTimes.put(dailyCustomTime.getId(), dailyCustomTime);

                                break;
                            }
                            case WEEKLY: {
                                WeeklySchedule weeklySchedule = (WeeklySchedule) schedule;

                                Pair<DayOfWeek, Time> pair = weeklySchedule.getDayOfWeekTime();

                                scheduleDatas.add(new CreateTaskLoader.WeeklyScheduleData(pair.first, pair.second.getTimePair()));

                                CustomTime weeklyCustomTime = pair.second.getPair().first;
                                if (weeklyCustomTime != null)
                                    customTimes.put(weeklyCustomTime.getId(), weeklyCustomTime);

                                break;
                            }
                            case MONTHLY_DAY: {
                                MonthlyDaySchedule monthlyDaySchedule = (MonthlyDaySchedule) schedule;

                                scheduleDatas.add(new CreateTaskLoader.MonthlyDayScheduleData(monthlyDaySchedule.getDayOfMonth(), monthlyDaySchedule.getBeginningOfMonth(), monthlyDaySchedule.getTime().getTimePair()));

                                CustomTime weeklyCustomTime = monthlyDaySchedule.getTime().getPair().first;
                                if (weeklyCustomTime != null)
                                    customTimes.put(weeklyCustomTime.getId(), weeklyCustomTime);

                                break;
                            }
                            case MONTHLY_WEEK: {
                                MonthlyWeekSchedule monthlyWeekSchedule = (MonthlyWeekSchedule) schedule;

                                scheduleDatas.add(new CreateTaskLoader.MonthlyWeekScheduleData(monthlyWeekSchedule.getDayOfMonth(), monthlyWeekSchedule.getDayOfWeek(), monthlyWeekSchedule.getBeginningOfMonth(), monthlyWeekSchedule.getTime().getTimePair()));

                                CustomTime weeklyCustomTime = monthlyWeekSchedule.getTime().getPair().first;
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
                MergedTask parentTask = task.getParentTask(now);
                Assert.assertTrue(parentTask != null);

                parentTaskKey = parentTask.getTaskKey();
            }

            taskData = new CreateTaskLoader.TaskData(task.getName(), parentTaskKey, scheduleDatas, task.getNote());
        }

        Map<TaskKey, CreateTaskLoader.TaskTreeData> taskDatas = getTaskDatas(context, now, excludedTaskKeys);

        @SuppressLint("UseSparseArrays") HashMap<Integer, CreateTaskLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes.values())
            customTimeDatas.put(customTime.getId(), new CreateTaskLoader.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));

        return new CreateTaskLoader.Data(taskData, taskDatas, customTimeDatas);
    }

    @NonNull
    public synchronized ShowTaskLoader.Data getShowTaskData(@NonNull TaskKey taskKey, @NonNull Context context) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowTaskData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        MergedTask task = getTask(taskKey);
        Assert.assertTrue(task.current(now));

        return new ShowTaskLoader.Data(task.isRootTask(now), task.getName(), task.getScheduleText(context, now), task.getTaskKey());
    }

    @NonNull
    public synchronized TaskListLoader.Data getTaskListData(@NonNull Context context, @Nullable TaskKey taskKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getTaskListData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        return getTaskListData(now, context, taskKey);
    }

    @NonNull
    TaskListLoader.Data getTaskListData(@NonNull ExactTimeStamp now, @NonNull Context context, @Nullable TaskKey taskKey) {
        List<TaskListLoader.ChildTaskData> childTaskDatas;
        String note;

        if (taskKey != null) {
            MergedTask parentTask = getTask(taskKey);

            List<MergedTask> tasks = parentTask.getChildTasks(now);
            childTaskDatas = Stream.of(tasks)
                    .map(task -> new TaskListLoader.ChildTaskData(task.getName(), task.getScheduleText(context, now), getChildTaskDatas(task, now, context), task.getNote(), task.getStartExactTimeStamp(), task.getTaskKey()))
                    .collect(Collectors.toList());

            note = parentTask.getNote();
        } else {
            childTaskDatas = Stream.of(getTasks().values())
                    .filter(task -> task.current(now))
                    .filter(task -> task.isVisible(now))
                    .filter(task -> task.isRootTask(now))
                    .map(task -> new TaskListLoader.ChildTaskData(task.getName(), task.getScheduleText(context, now), getChildTaskDatas(task, now, context), task.getNote(), task.getStartExactTimeStamp(), task.getTaskKey()))
                    .collect(Collectors.toList());

            note = null;
        }

        Collections.sort(childTaskDatas, (TaskListLoader.ChildTaskData lhs, TaskListLoader.ChildTaskData rhs) -> lhs.mStartExactTimeStamp.compareTo(rhs.mStartExactTimeStamp));
        if (taskKey == null)
            Collections.reverse(childTaskDatas);

        return new TaskListLoader.Data(childTaskDatas, note);
    }

    // sets

    public synchronized void setInstanceDateTime(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.log("DomainFactory.setInstanceDateTime");

        MergedInstance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now);

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized void setInstancesDateTime(@NonNull Context context, int dataId, @NonNull Set<InstanceKey> instanceKeys, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.log("DomainFactory.setInstancesDateTime");

        Assert.assertTrue(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (InstanceKey instanceKey : instanceKeys) {
            MergedInstance instance = getInstance(instanceKey);
            instance.setInstanceDateTime(instanceDate, instanceTimePair, now);
        }

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized void setInstanceAddHour(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHour");

        MergedInstance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);
        instance.setNotificationShown(false, now);

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized void setInstanceNotificationDone(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotificationDone");

        MergedInstance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setDone(true, now);
        instance.setNotificationShown(false, now);
        instance.setNotified(now);

        save(context, dataId);
    }

    @NonNull
    public synchronized ExactTimeStamp setInstancesDone(@NonNull Context context, int dataId, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.log("DomainFactory.setInstancesDone");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Stream.of(instanceKeys).forEach(instanceKey -> {
            Assert.assertTrue(instanceKey != null);

            MergedInstance instance = getInstance(instanceKey);

            instance.setDone(true, now);
        });

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);

        return now;
    }

    public synchronized ExactTimeStamp setInstanceDone(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey, boolean done) {
        MyCrashlytics.log("DomainFactory.setInstanceDone");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        MergedInstance instance = setInstanceDone(now, instanceKey, done);

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);

        return instance.getDone();
    }

    @NonNull
    MergedInstance setInstanceDone(@NonNull ExactTimeStamp now, @NonNull InstanceKey instanceKey, boolean done) {
        MergedInstance instance = getInstance(instanceKey);

        instance.setDone(done, now);

        return instance;
    }

    public synchronized void setInstancesNotified(@NonNull Context context, int dataId, @NonNull ArrayList<InstanceKey> instanceKeys) {
        MyCrashlytics.log("DomainFactory.setInstancesNotified");

        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (InstanceKey instanceKey : instanceKeys) {
            MergedInstance instance = getInstance(instanceKey);

            instance.setNotified(now);
            instance.setNotificationShown(false, now);
        }

        save(context, dataId);
    }

    public synchronized void setInstanceNotified(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotified");

        MergedInstance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setNotified(now);
        instance.setNotificationShown(false, now);

        save(context, dataId);
    }

    @NonNull
    Task createScheduleRootTask(@NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        Task rootTask = createLocalTaskHelper(name, now, note);

        List<Schedule> schedules = createSchedules(rootTask, scheduleDatas, now);
        Assert.assertTrue(!schedules.isEmpty());

        rootTask.addSchedules(schedules);

        return rootTask;
    }

    public synchronized void createScheduleRootTask(@NonNull Context context, int dataId, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createScheduleRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        createScheduleRootTask(now, name, scheduleDatas, note);

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized void createScheduleRootTask(@NonNull Context context, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull List<UserData> friendEntries) {
        MyCrashlytics.log("DomainFactory.createScheduleRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(!friendEntries.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        mRemoteFactory.createScheduleRootTask(this, now, name, scheduleDatas, note, friendEntries);

        updateNotifications(context, new ArrayList<>(), now);
    }

    public synchronized void updateScheduleTask(@NonNull Context context, int dataId, int taskId, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.updateScheduleTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        Task task = mTasks.get(taskId); // todo firebase
        Assert.assertTrue(task != null);

        List<TaskKey> taskKeys = new ArrayList<>();
        taskKeys.add(task.getTaskKey());

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Assert.assertTrue(task.current(now));

        task.setName(name, note);

        if (task.isRootTask(now)) {
            List<Schedule> schedules = task.getCurrentSchedules(now);

            Stream.of(schedules)
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));
        } else {
            LocalTaskHierarchy localTaskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(localTaskHierarchy != null);

            localTaskHierarchy.setEndExactTimeStamp(now);

            taskKeys.add(localTaskHierarchy.getParentTaskKey());
        }

        List<Schedule> schedules = createSchedules(task, scheduleDatas, now);
        Assert.assertTrue(!schedules.isEmpty());

        task.addSchedules(schedules);

        updateNotifications(context, taskKeys, now);

        save(context, dataId);
    }

    private void createScheduleJoinRootTask(@NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull List<Integer> joinTaskIds, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(joinTaskIds.size() > 1);

        Task rootTask = createLocalTaskHelper(name, now, note);

        List<Schedule> schedules = createSchedules(rootTask, scheduleDatas, now);
        Assert.assertTrue(!schedules.isEmpty());

        rootTask.addSchedules(schedules);

        joinTasks(rootTask, joinTaskIds, now);
    }

    public synchronized void createScheduleJoinRootTask(@NonNull Context context, int dataId, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull List<Integer> joinTaskIds, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<TaskKey> taskKeys = Stream.of(joinTaskIds)
                .map(TaskKey::new)
                .map(this::getTask)
                .map(task -> task.getParentTask(now))
                .filter(parentTask -> parentTask != null)
                .map(MergedTask::getTaskKey)
                .collect(Collectors.toList());

        createScheduleJoinRootTask(now, name, scheduleDatas, joinTaskIds, note);

        updateNotifications(context, taskKeys, now);

        save(context, dataId);
    }

    public synchronized void createChildTask(@NonNull Context context, int dataId, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createChildTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        if (parentTaskKey.mLocalTaskId != null) { // todo firebase?
            Assert.assertTrue(TextUtils.isEmpty(parentTaskKey.mRemoteTaskId));

            MergedTask parentTask = getTask(parentTaskKey);
            Assert.assertTrue(parentTask.current(now));

            parentTask.createChildTask(now, name, note);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(parentTaskKey.mRemoteTaskId));

            MergedTask parentTask = getTask(parentTaskKey);
            Assert.assertTrue(parentTask.current(now));

            parentTask.createChildTask(now, name, note);
        }

        updateNotifications(context, Collections.singletonList(parentTaskKey), now);

        save(context, dataId);
    }

    @NonNull
    Task createLocalChildTask(@NonNull ExactTimeStamp now, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        Assert.assertTrue(parentTaskKey.mLocalTaskId != null);
        Assert.assertTrue(TextUtils.isEmpty(parentTaskKey.mRemoteTaskId));
        Assert.assertTrue(!TextUtils.isEmpty(name));

        MergedTask parentTask = getTask(parentTaskKey);
        Assert.assertTrue(parentTask.current(now));
        Assert.assertTrue(parentTask instanceof Task);

        Task childTask = createLocalTaskHelper(name, now, note);

        createTaskHierarchy((Task) parentTask, childTask, now);

        return childTask;
    }

    public synchronized void createJoinChildTask(@NonNull Context context, int dataId, int parentTaskId, @NonNull String name, @NonNull List<Integer> joinTaskIds, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createJoinChildTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<TaskKey> taskKeys = Stream.of(joinTaskIds)
                .map(TaskKey::new)
                .map(this::getTask)
                .map(task -> task.getParentTask(now))
                .filter(parentTask -> parentTask != null)
                .map(MergedTask::getTaskKey)
                .collect(Collectors.toList());

        Task parentTask = mTasks.get(parentTaskId); // todo firebase
        Assert.assertTrue(parentTask != null);
        Assert.assertTrue(parentTask.current(now));

        taskKeys.add(parentTask.getTaskKey());

        TaskRecord childTaskRecord = mPersistenceManager.createTaskRecord(name, now, note);

        Task childTask = new Task(this, childTaskRecord);
        Assert.assertTrue(!mTasks.containsKey(childTask.getId())); // todo firebase
        mTasks.put(childTask.getId(), childTask); // todo firebase

        createTaskHierarchy(parentTask, childTask, now);

        joinTasks(childTask, joinTaskIds, now);

        updateNotifications(context, taskKeys, now);

        save(context, dataId);
    }

    public synchronized void updateChildTask(@NonNull Context context, int dataId, int taskId, @NonNull String name, int parentTaskId, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.updateChildTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<TaskKey> taskKeys = new ArrayList<>();

        Task task = mTasks.get(taskId); // todo firebase
        Assert.assertTrue(task != null);

        taskKeys.add(task.getTaskKey());

        task.setName(name, note);

        Task newParentTask = mTasks.get(parentTaskId); // todo firebase
        Assert.assertTrue(newParentTask != null);

        taskKeys.add(newParentTask.getTaskKey());

        MergedTask oldParentTask = task.getParentTask(now);
        if (oldParentTask == null) {
            Stream.of(task.getCurrentSchedules(now))
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));

            createTaskHierarchy(newParentTask, task, now);
        } else if (oldParentTask != newParentTask) {
            LocalTaskHierarchy oldLocalTaskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(oldLocalTaskHierarchy != null);

            oldLocalTaskHierarchy.setEndExactTimeStamp(now);

            taskKeys.add(oldLocalTaskHierarchy.getParentTaskKey());

            createTaskHierarchy(newParentTask, task, now);
        }

        updateNotifications(context, taskKeys, now);

        save(context, dataId);
    }

    public synchronized void setTaskEndTimeStamp(@NonNull Context context, @NonNull ArrayList<Integer> dataIds, @NonNull TaskKey taskKey) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamp");

        Assert.assertTrue(!dataIds.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        MergedTask task = getTask(taskKey);
        Assert.assertTrue(task.current(now));

        task.setEndExactTimeStamp(now);

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataIds);
    }

    public synchronized void setTaskEndTimeStamps(@NonNull Context context, int dataId, @NonNull ArrayList<TaskKey> taskKeys) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps");

        Assert.assertTrue(!taskKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (TaskKey taskKey : taskKeys) {
            Assert.assertTrue(taskKey != null);

            MergedTask task = getTask(taskKey);
            Assert.assertTrue(task.current(now));

            task.setEndExactTimeStamp(now);
        }

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized int createCustomTime(@NonNull Context context, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.log("DomainFactory.createCustomTime");

        Assert.assertTrue(!TextUtils.isEmpty(name));

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

        save(context, 0);

        return customTime.getId();
    }

    public synchronized void updateCustomTime(@NonNull Context context, int dataId, int customTimeId, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.log("DomainFactory.updateCustomTime");

        Assert.assertTrue(!TextUtils.isEmpty(name));

        CustomTime customTime = mCustomTimes.get(customTimeId);
        Assert.assertTrue(customTime != null);

        customTime.setName(name);

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            HourMinute hourMinute = hourMinutes.get(dayOfWeek);
            Assert.assertTrue(hourMinute != null);

            if (hourMinute.compareTo(customTime.getHourMinute(dayOfWeek)) != 0)
                customTime.setHourMinute(dayOfWeek, hourMinute);
        }

        save(context, dataId);
    }

    public synchronized void setCustomTimeCurrent(@NonNull Context context, int dataId, @NonNull List<Integer> customTimeIds) {
        MyCrashlytics.log("DomainFactory.setCustomTimeCurrent");

        Assert.assertTrue(!customTimeIds.isEmpty());

        for (int customTimeId : customTimeIds) {
            CustomTime customTime = mCustomTimes.get(customTimeId);
            Assert.assertTrue(customTime != null);

            customTime.setCurrent();
        }

        save(context, dataId);
    }

    @NonNull
    Irrelevant setIrrelevant(@NonNull ExactTimeStamp now) {
        for (MergedTask task : getTasks().values())
            task.updateOldestVisible(now);

        // relevant hack
        Map<TaskKey, TaskRelevance> taskRelevances = Stream.of(getTasks().values()).collect(Collectors.toMap(MergedTask::getTaskKey, TaskRelevance::new));
        Map<InstanceKey, InstanceRelevance> instanceRelevances = Stream.of(getExistingInstances().values()).collect(Collectors.toMap(MergedInstance::getInstanceKey, InstanceRelevance::new));
        Map<Integer, CustomTimeRelevance> customTimeRelevances = Stream.of(mCustomTimes.values()).collect(Collectors.toMap(CustomTime::getId, CustomTimeRelevance::new));

        Stream.of(getTasks().values())
                .filter(task -> task.current(now))
                .filter(task -> task.isRootTask(now))
                .filter(task -> task.isVisible(now))
                .map(MergedTask::getTaskKey)
                .map(taskRelevances::get)
                .forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        Stream.of(getRootInstances(null, now.plusOne(), now))
                .map(MergedInstance::getInstanceKey)
                .map(instanceRelevances::get)
                .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        Stream.of(getExistingInstances().values())
                .filter(instance -> instance.isRootInstance(now))
                .filter(instance -> instance.isVisible(now))
                .map(MergedInstance::getInstanceKey)
                .map(instanceRelevances::get)
                .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        Stream.of(getCurrentCustomTimes())
                .map(CustomTime::getId)
                .map(customTimeRelevances::get)
                .forEach(CustomTimeRelevance::setRelevant);

        List<MergedTask> relevantTasks = Stream.of(taskRelevances.values())
                .filter(TaskRelevance::getRelevant)
                .map(TaskRelevance::getTask)
                .collect(Collectors.toList());

        List<MergedTask> irrelevantTasks = new ArrayList<>(getTasks().values());
        irrelevantTasks.removeAll(relevantTasks);

        Assert.assertTrue(Stream.of(irrelevantTasks)
                .noneMatch(task -> task.isVisible(now)));

        List<MergedInstance> relevantExistingInstances = Stream.of(instanceRelevances.values())
                .filter(InstanceRelevance::getRelevant)
                .map(InstanceRelevance::getInstance)
                .filter(MergedInstance::exists)
                .collect(Collectors.toList());

        List<MergedInstance> irrelevantExistingInstances = new ArrayList<>(getExistingInstances().values());
        irrelevantExistingInstances.removeAll(relevantExistingInstances);

        Assert.assertTrue(Stream.of(irrelevantExistingInstances)
                .noneMatch(instance -> instance.isVisible(now)));

        List<CustomTime> relevantCustomTimes = Stream.of(customTimeRelevances.values())
                .filter(CustomTimeRelevance::getRelevant)
                .map(CustomTimeRelevance::getCustomTime)
                .collect(Collectors.toList());

        List<CustomTime> irrelevantCustomTimes = new ArrayList<>(mCustomTimes.values());
        irrelevantCustomTimes.removeAll(relevantCustomTimes);

        Assert.assertTrue(Stream.of(irrelevantCustomTimes)
                .noneMatch(CustomTime::getCurrent));

        Stream.of(irrelevantTasks)
                .forEach(MergedTask::setRelevant);

        Stream.of(irrelevantExistingInstances)
                .forEach(MergedInstance::setRelevant);

        Stream.of(irrelevantCustomTimes)
                .forEach(CustomTime::setRelevant);

        return new Irrelevant(irrelevantCustomTimes, irrelevantTasks, irrelevantExistingInstances);
    }

    void removeIrrelevant(@NonNull Irrelevant irrelevant) {
        List<LocalTaskHierarchy> irrelevantTaskHierarchies = Stream.of(mTaskHierarchies.values()) // todo removal
                .filter(taskHierarchy -> irrelevant.mTasks.contains(taskHierarchy.getChildTask()))
                .collect(Collectors.toList());

        Assert.assertTrue(Stream.of(irrelevantTaskHierarchies)
                .allMatch(taskHierarchy -> irrelevant.mTasks.contains(taskHierarchy.getParentTask())));

        for (LocalTaskHierarchy irrelevantLocalTaskHierarchy : irrelevantTaskHierarchies)
            mTaskHierarchies.remove(irrelevantLocalTaskHierarchy.getId()); // todo removal

        for (MergedTask task : irrelevant.mTasks) {
            if (task instanceof Task) {
                Assert.assertTrue(mTasks.containsKey(((Task) task).getId())); // todo removal

                mTasks.remove(((Task) task).getId()); // todo removal
            }
        }

        for (MergedInstance instance : irrelevant.mInstances) {
            if (instance instanceof Instance) {
                Assert.assertTrue(mExistingInstances.contains(instance));

                mExistingInstances.remove(instance);
            }
        }

        Stream.of(irrelevant.mCustomTimes)
                .forEach(mCustomTimes::remove); // todo customTimes

        mRemoteFactory.removeIrrelevant(irrelevant);
    }

    public synchronized void createRootTask(@NonNull Context context, int dataId, @NonNull String name, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        TaskRecord taskRecord = mPersistenceManager.createTaskRecord(name, now, note);

        Task childTask = new Task(this, taskRecord);
        Assert.assertTrue(!mTasks.containsKey(childTask.getId())); // todo firebase
        mTasks.put(childTask.getId(), childTask); // todo firbase

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized void createJoinRootTask(@NonNull Context context, int dataId, @NonNull String name, @NonNull List<Integer> joinTaskIds, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createJoinRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<TaskKey> taskKeys = Stream.of(joinTaskIds)
                .map(TaskKey::new)
                .map(this::getTask)
                .map(task -> task.getParentTask(now))
                .filter(parentTask -> parentTask != null)
                .map(MergedTask::getTaskKey)
                .collect(Collectors.toList());

        TaskRecord taskRecord = mPersistenceManager.createTaskRecord(name, now, note);

        Task task = new Task(this, taskRecord);
        Assert.assertTrue(!mTasks.containsKey(task.getId())); // todo firebase
        mTasks.put(task.getId(), task); // todo firebase

        joinTasks(task, joinTaskIds, now);

        updateNotifications(context, taskKeys, now);

        save(context, dataId);
    }

    public synchronized void updateRootTask(@NonNull Context context, int dataId, int taskId, @NonNull String name, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.updateRootTask");

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = mTasks.get(taskId);  // todo firebase
        Assert.assertTrue(task != null);

        task.setName(name, note);

        List<TaskKey> taskKeys = new ArrayList<>();

        LocalTaskHierarchy localTaskHierarchy = getParentTaskHierarchy(task, now);
        if (localTaskHierarchy != null) {
            localTaskHierarchy.setEndExactTimeStamp(now);
            taskKeys.add(localTaskHierarchy.getParentTaskKey());
        }

        Stream.of(task.getCurrentSchedules(now))
                .forEach(schedule -> schedule.setEndExactTimeStamp(now));

        updateNotifications(context, taskKeys, now);

        save(context, dataId);
    }

    public synchronized void updateNotifications(@NonNull Context context, boolean silent, boolean registering, @NonNull List<TaskKey> taskKeys) {
        ExactTimeStamp now = ExactTimeStamp.getNow();

        updateNotifications(context, silent, registering, taskKeys, now);

        Irrelevant irrelevant = setIrrelevant(now);

        save(context, 0);

        removeIrrelevant(irrelevant);
    }

    private void updateNotifications(@NonNull Context context, @NonNull List<TaskKey> taskKeys, @NonNull ExactTimeStamp now) {
        updateNotifications(context, true, false, taskKeys, now);
    }

    private void updateNotifications(@NonNull Context context, boolean silent, boolean registering, @NonNull List<TaskKey> taskKeys, @NonNull ExactTimeStamp now) {
        if (!silent) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(TickService.TICK_PREFERENCES, Context.MODE_PRIVATE);
            sharedPreferences.edit().putLong(TickService.LAST_TICK_KEY, ExactTimeStamp.getNow().getLong()).apply();
        }

        List<MergedInstance> rootInstances = getRootInstances(null, now.plusOne(), now); // 24 hack

        Map<InstanceKey, MergedInstance> notificationInstances = Stream.of(rootInstances)
                .filter(instance -> (instance.getDone() == null) && !instance.getNotified() && instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0)
                .collect(Collectors.toMap(MergedInstance::getInstanceKey, instance -> instance));

        Map<InstanceKey, Instance> shownInstances = Stream.of(mExistingInstances)
                .filter(Instance::getNotificationShown)
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> instance));

        Set<InstanceKey> shownInstanceKeys = shownInstances.keySet();

        List<InstanceKey> showInstanceKeys = Stream.of(notificationInstances.keySet())
                .filter(instanceKey -> !shownInstanceKeys.contains(instanceKey))
                .collect(Collectors.toList());

        List<InstanceKey> hideInstanceKeys = Stream.of(shownInstances.keySet())
                .filter(instanceKey -> !notificationInstances.containsKey(instanceKey))
                .collect(Collectors.toList());

        if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty()) {
            for (InstanceKey showInstanceKey : showInstanceKeys) {
                Assert.assertTrue(showInstanceKey != null);

                MergedInstance showInstance = getInstance(showInstanceKey);

                showInstance.setNotificationShown(true, now);
            }

            for (InstanceKey hideInstanceKey : hideInstanceKeys) {
                Assert.assertTrue(hideInstanceKey != null);

                MergedInstance hideInstance = getInstance(hideInstanceKey);

                hideInstance.setNotificationShown(false, now);
            }
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Assert.assertTrue(notificationManager != null);

        if (registering) {
            Assert.assertTrue(silent);

            if (notificationInstances.size() > TickService.MAX_NOTIFICATIONS) { // show group
                notifyGroup(context, notificationInstances.values(), true, now);
            } else { // show instances
                for (MergedInstance instance : notificationInstances.values()) {
                    Assert.assertTrue(instance != null);

                    notifyInstance(context, instance, true, now);
                }
            }
        } else {
            if (notificationInstances.size() > TickService.MAX_NOTIFICATIONS) { // show group
                if (shownInstances.size() > TickService.MAX_NOTIFICATIONS) { // group shown
                    if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty()) {
                        notifyGroup(context, notificationInstances.values(), silent, now);
                    } else if (Stream.of(notificationInstances.values()).anyMatch(instance -> updateInstance(taskKeys, instance, now))) {
                        notifyGroup(context, notificationInstances.values(), true, now);
                    }
                } else { // instances shown
                    for (MergedInstance instance : shownInstances.values())
                        notificationManager.cancel(instance.getNotificationId());

                    notifyGroup(context, notificationInstances.values(), silent, now);
                }
            } else { // show instances
                if (shownInstances.size() > TickService.MAX_NOTIFICATIONS) { // group shown
                    notificationManager.cancel(0);

                    for (MergedInstance instance : notificationInstances.values()) {
                        Assert.assertTrue(instance != null);

                        notifyInstance(context, instance, silent, now);
                    }
                } else { // instances shown
                    for (InstanceKey hideInstanceKey : hideInstanceKeys) {
                        MergedInstance instance = shownInstances.get(hideInstanceKey);
                        Assert.assertTrue(instance != null);

                        notificationManager.cancel(instance.getNotificationId());
                    }

                    for (InstanceKey showInstanceKey : showInstanceKeys) {
                        MergedInstance instance = notificationInstances.get(showInstanceKey);
                        Assert.assertTrue(instance != null);

                        notifyInstance(context, instance, silent, now);
                    }

                    Stream.of(notificationInstances.values())
                            .filter(instance -> updateInstance(taskKeys, instance, now))
                            .filter(instance -> !showInstanceKeys.contains(instance.getInstanceKey()))
                            .forEach(instance -> notifyInstance(context, instance, true, now));
                }
            }
        }

        TimeStamp nextAlarm = null;
        for (Instance existingInstance : mExistingInstances) {
            TimeStamp instanceTimeStamp = existingInstance.getInstanceDateTime().getTimeStamp();
            if (instanceTimeStamp.toExactTimeStamp().compareTo(now) > 0)
                if (nextAlarm == null || instanceTimeStamp.compareTo(nextAlarm) < 0)
                    nextAlarm = instanceTimeStamp;
        }

        for (MergedTask task : getTasks().values()) {
            if (task.current(now) && task.isRootTask(now)) {
                List<? extends MergedSchedule> schedules = task.getCurrentSchedules(now);

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

        if (nextAlarm != null) {
            Intent nextIntent = TickService.getIntent(context, false, false, new ArrayList<>());

            PendingIntent pendingIntent = PendingIntent.getService(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Assert.assertTrue(pendingIntent != null);

            setExact(context, nextAlarm.getLong(), pendingIntent);
        }
    }

    private boolean updateInstance(@NonNull List<TaskKey> taskKeys, @NonNull MergedInstance instance, @NonNull ExactTimeStamp now) {
        return (taskKeys.contains(instance.getTaskKey()) || Stream.of(instance.getChildInstances(now)).anyMatch(childInstance -> taskKeys.contains(childInstance.getTaskKey())));
    }

    // internal

    @NonNull
    private ArrayList<MergedInstance> getExistingInstances(@NonNull MergedTask task) {
        ArrayList<MergedInstance> instances = new ArrayList<>();
        for (MergedInstance instance : getExistingInstances().values()) {
            Assert.assertTrue(instance != null);
            if (instance.getTaskKey().equals(task.getTaskKey()))
                instances.add(instance);
        }

        return instances;
    }

    @Nullable
    public MergedInstance getExistingInstance(@NonNull MergedTask task, @NonNull DateTime scheduleDateTime) {
        ArrayList<MergedInstance> taskInstances = getExistingInstances(task);

        ArrayList<MergedInstance> instances = new ArrayList<>();
        for (MergedInstance instance : taskInstances) {
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

    @NonNull
    public MergedInstance getInstance(@NonNull MergedTask task, @NonNull DateTime scheduleDateTime) {
        MergedInstance existingInstance = getExistingInstance(task, scheduleDateTime);

        if (existingInstance != null) {
            return existingInstance;
        } else {
            if (task.getTaskKey().mLocalTaskId != null) {
                Assert.assertTrue(TextUtils.isEmpty(task.getTaskKey().mRemoteTaskId));

                return new Instance(this, task.getTaskKey().mLocalTaskId, scheduleDateTime);
            } else {
                Assert.assertTrue(!TextUtils.isEmpty(task.getTaskKey().mRemoteTaskId));

                HourMinute hourMinute = scheduleDateTime.getTime().getTimePair().mHourMinute;
                Integer hour = (hourMinute != null ? hourMinute.getHour() : null);
                Integer minute = (hourMinute != null ? hourMinute.getMinute() : null);
                InstanceShownRecord instanceShownRecord = getInstanceShownRecord(task.getTaskKey().mRemoteTaskId, scheduleDateTime.getDate().getYear(), scheduleDateTime.getDate().getMonth(), scheduleDateTime.getDate().getDay(), scheduleDateTime.getTime().getTimePair().mCustomTimeId, hour, minute);

                return new RemoteInstance(this, task.getTaskKey().mRemoteTaskId, scheduleDateTime, instanceShownRecord);
            }
        }
    }

    @NonNull
    public List<MergedInstance> getPastInstances(@NonNull MergedTask task, @NonNull ExactTimeStamp now) {
        Map<InstanceKey, MergedInstance> allInstances = new HashMap<>();

        allInstances.putAll(Stream.of(getExistingInstances().values())
                .filter(instance -> instance.getTaskKey().equals(task.getTaskKey()))
                .filter(instance -> instance.getScheduleDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0)
                .collect(Collectors.toMap(MergedInstance::getInstanceKey, instance -> instance)));

        allInstances.putAll(Stream.of(task.getInstances(null, now.plusOne(), now))
                .collect(Collectors.toMap(MergedInstance::getInstanceKey, instance -> instance)));

        return new ArrayList<>(allInstances.values());
    }

    @NonNull
    private List<MergedInstance> getRootInstances(@Nullable ExactTimeStamp startExactTimeStamp, @NonNull ExactTimeStamp endExactTimeStamp, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(startExactTimeStamp == null || startExactTimeStamp.compareTo(endExactTimeStamp) < 0);

        Map<InstanceKey, MergedInstance> allInstances = new HashMap<>();

        for (Instance instance : mExistingInstances) {
            ExactTimeStamp instanceExactTimeStamp = instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp();

            if (startExactTimeStamp != null && startExactTimeStamp.compareTo(instanceExactTimeStamp) > 0)
                continue;

            if (endExactTimeStamp.compareTo(instanceExactTimeStamp) <= 0)
                continue;

            allInstances.put(instance.getInstanceKey(), instance);
        }

        for (MergedTask task : getTasks().values()) {
            for (MergedInstance instance : task.getInstances(startExactTimeStamp, endExactTimeStamp, now)) {
                ExactTimeStamp instanceExactTimeStamp = instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp();

                if (startExactTimeStamp != null && startExactTimeStamp.compareTo(instanceExactTimeStamp) > 0)
                    continue;

                if (endExactTimeStamp.compareTo(instanceExactTimeStamp) <= 0)
                    continue;

                allInstances.put(instance.getInstanceKey(), instance);
            }
        }

        return Stream.of(allInstances.values())
                .filter(instance -> instance.isRootInstance(now))
                .filter(instance -> instance.isVisible(now))
                .collect(Collectors.toList());
    }

    @NonNull
    InstanceRecord createInstanceRecord(@NonNull Task task, @NonNull Instance instance, @NonNull DateTime scheduleDateTime, @NonNull ExactTimeStamp now) {
        mExistingInstances.add(instance);

        return mPersistenceManager.createInstanceRecord(task, scheduleDateTime, now);
    }

    @NonNull
    private DateTime getDateTime(@NonNull Date date, @NonNull TimePair timePair) {
        Time time = getTime(timePair);

        return new DateTime(date, time);
    }

    @NonNull
    private MergedInstance getInstance(@NonNull InstanceKey instanceKey) {
        MergedTask task = getTask(instanceKey.mTaskKey);

        DateTime scheduleDateTime = getDateTime(instanceKey.ScheduleDate, instanceKey.ScheduleTimePair);

        return getInstance(task, scheduleDateTime);
    }

    @NonNull
    private ArrayList<Schedule> loadSchedules(@NonNull Task task) {
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
                case MONTHLY_DAY:
                    schedules.add(loadMonthlyDaySchedule(scheduleRecord, task));
                    break;
                case MONTHLY_WEEK:
                    schedules.add(loadMonthlyWeekSchedule(scheduleRecord, task));
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

        return new SingleSchedule(this, scheduleRecord, singleScheduleRecord);
    }

    private DailySchedule loadDailySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        DailyScheduleRecord dailyScheduleRecord = mPersistenceManager.getDailyScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(dailyScheduleRecord != null);

        return new DailySchedule(this, scheduleRecord, dailyScheduleRecord);
    }

    private WeeklySchedule loadWeeklySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        WeeklyScheduleRecord weeklyScheduleRecord = mPersistenceManager.getWeeklyScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(weeklyScheduleRecord != null);

        return new WeeklySchedule(this, scheduleRecord, weeklyScheduleRecord);
    }

    private MonthlyDaySchedule loadMonthlyDaySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        MonthlyDayScheduleRecord monthlyDayScheduleRecord = mPersistenceManager.getMonthlyDayScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(monthlyDayScheduleRecord != null);

        return new MonthlyDaySchedule(this, scheduleRecord, monthlyDayScheduleRecord);
    }

    private MonthlyWeekSchedule loadMonthlyWeekSchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        MonthlyWeekScheduleRecord monthlyWeekScheduleRecord = mPersistenceManager.getMonthlyWeekScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(monthlyWeekScheduleRecord != null);

        return new MonthlyWeekSchedule(this, scheduleRecord, monthlyWeekScheduleRecord);
    }

    @NonNull
    private Task createLocalTaskHelper(@NonNull String name, @NonNull ExactTimeStamp startExactTimeStamp, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        TaskRecord taskRecord = mPersistenceManager.createTaskRecord(name, startExactTimeStamp, note);

        Task rootTask = new Task(this, taskRecord);

        Assert.assertTrue(!mTasks.containsKey(rootTask.getId()));
        mTasks.put(rootTask.getId(), rootTask);

        return rootTask;
    }

    @NonNull
    private Time getTime(@NonNull TimePair timePair) {
        if (timePair.mCustomTimeId != null) {
            Assert.assertTrue(timePair.mHourMinute == null);

            CustomTime customTime = mCustomTimes.get(timePair.mCustomTimeId);
            Assert.assertTrue(customTime != null);

            return customTime;
        } else {
            Assert.assertTrue(timePair.mHourMinute != null);
            return new NormalTime(timePair.mHourMinute);
        }
    }

    private void joinTasks(@NonNull Task newParentTask, @NonNull List<Integer> joinTaskIds, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(newParentTask.current(now));
        Assert.assertTrue(joinTaskIds.size() > 1);

        for (int joinTaskId : joinTaskIds) {
            Task joinTask = mTasks.get(joinTaskId); // todo firebase
            Assert.assertTrue(joinTask != null);
            Assert.assertTrue(joinTask.current(now));

            if (joinTask.isRootTask(now)) {
                Stream.of(joinTask.getCurrentSchedules(now))
                        .forEach(schedule -> schedule.setEndExactTimeStamp(now));
            } else {
                LocalTaskHierarchy localTaskHierarchy = getParentTaskHierarchy(joinTask, now);
                Assert.assertTrue(localTaskHierarchy != null);

                localTaskHierarchy.setEndExactTimeStamp(now);
            }

            createTaskHierarchy(newParentTask, joinTask, now);
        }
    }

    private void createTaskHierarchy(@NonNull Task parentTask, @NonNull Task childTask, @NonNull ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(parentTask.current(startExactTimeStamp));
        Assert.assertTrue(childTask.current(startExactTimeStamp));

        TaskHierarchyRecord taskHierarchyRecord = mPersistenceManager.createTaskHierarchyRecord(parentTask, childTask, startExactTimeStamp);
        Assert.assertTrue(taskHierarchyRecord != null);

        LocalTaskHierarchy localTaskHierarchy = new LocalTaskHierarchy(this, taskHierarchyRecord);
        Assert.assertTrue(!mTaskHierarchies.containsKey(localTaskHierarchy.getId())); // todo firebase
        mTaskHierarchies.put(localTaskHierarchy.getId(), localTaskHierarchy); // todo firebase
    }

    @NonNull
    private List<Schedule> createSchedules(@NonNull Task rootTask, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(rootTask.current(startExactTimeStamp));

        List<Schedule> schedules = new ArrayList<>();

        for (CreateTaskLoader.ScheduleData scheduleData : scheduleDatas) {
            Assert.assertTrue(scheduleData != null);

            switch (scheduleData.getScheduleType()) {
                case SINGLE: {
                    CreateTaskLoader.SingleScheduleData singleScheduleData = (CreateTaskLoader.SingleScheduleData) scheduleData;

                    Date date = singleScheduleData.Date;
                    Time time = getTime(singleScheduleData.TimePair);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.SINGLE, startExactTimeStamp);

                    SingleScheduleRecord singleScheduleRecord = mPersistenceManager.createSingleScheduleRecord(scheduleRecord.getId(), date, time);

                    schedules.add(new SingleSchedule(this, scheduleRecord, singleScheduleRecord));
                    break;
                }
                case DAILY: {
                    CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                    Time time = getTime(dailyScheduleData.TimePair);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.DAILY, startExactTimeStamp);

                    DailyScheduleRecord dailyScheduleRecord = mPersistenceManager.createDailyScheduleRecord(scheduleRecord.getId(), time);

                    schedules.add(new DailySchedule(this, scheduleRecord, dailyScheduleRecord));
                    break;
                }
                case WEEKLY: {
                    CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;

                    DayOfWeek dayOfWeek = weeklyScheduleData.DayOfWeek;
                    Time time = getTime(weeklyScheduleData.TimePair);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.WEEKLY, startExactTimeStamp);

                    WeeklyScheduleRecord weeklyScheduleRecord = mPersistenceManager.createWeeklyScheduleRecord(scheduleRecord.getId(), dayOfWeek, time);

                    schedules.add(new WeeklySchedule(this, scheduleRecord, weeklyScheduleRecord));
                    break;
                }
                case MONTHLY_DAY: {
                    CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.MONTHLY_DAY, startExactTimeStamp);

                    MonthlyDayScheduleRecord monthlyDayScheduleRecord = mPersistenceManager.createMonthlyDayScheduleRecord(scheduleRecord.getId(), monthlyDayScheduleData.mDayOfMonth, monthlyDayScheduleData.mBeginningOfMonth, getTime(monthlyDayScheduleData.TimePair));

                    schedules.add(new MonthlyDaySchedule(this, scheduleRecord, monthlyDayScheduleRecord));
                    break;
                }
                case MONTHLY_WEEK: {
                    CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootTask, ScheduleType.MONTHLY_WEEK, startExactTimeStamp);

                    MonthlyWeekScheduleRecord monthlyWeekScheduleRecord = mPersistenceManager.createMonthlyWeekScheduleRecord(scheduleRecord.getId(), monthlyWeekScheduleData.mDayOfMonth, monthlyWeekScheduleData.mDayOfWeek, monthlyWeekScheduleData.mBeginningOfMonth, getTime(monthlyWeekScheduleData.TimePair));

                    schedules.add(new MonthlyWeekSchedule(this, scheduleRecord, monthlyWeekScheduleRecord));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        return schedules;
    }

    @NonNull
    public List<TaskHierarchy> getParentTaskHierarchies(MergedTask childTask) {
        Assert.assertTrue(childTask != null);

        return Stream.of(getTaskHierarchies())
                .filter(taskHierarchy -> taskHierarchy.getChildTask() == childTask)
                .collect(Collectors.toList());
    }

    @Nullable
    public MergedTask getParentTask(@NonNull MergedTask childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(childTask.current(exactTimeStamp));

        TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, exactTimeStamp);
        if (parentTaskHierarchy == null) {
            return null;
        } else {
            Assert.assertTrue(parentTaskHierarchy.current(exactTimeStamp));
            MergedTask parentTask = parentTaskHierarchy.getParentTask();
            Assert.assertTrue(parentTask.current(exactTimeStamp));
            return parentTask;
        }
    }

    @Nullable
    private LocalTaskHierarchy getParentTaskHierarchy(@NonNull Task childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(childTask.current(exactTimeStamp));

        ArrayList<LocalTaskHierarchy> taskHierarchies = new ArrayList<>();
        for (LocalTaskHierarchy localTaskHierarchy : mTaskHierarchies.values()) {
            Assert.assertTrue(localTaskHierarchy != null);

            if (!localTaskHierarchy.current(exactTimeStamp))
                continue;

            if (localTaskHierarchy.getChildTask() != childTask)
                continue;

            taskHierarchies.add(localTaskHierarchy);
        }

        if (taskHierarchies.isEmpty()) {
            return null;
        } else {
            Assert.assertTrue(taskHierarchies.size() == 1);
            return taskHierarchies.get(0);
        }
    }

    void setParentHierarchyEndTimeStamp(@NonNull Task childTask, @NonNull ExactTimeStamp exactEndTimeStamp) {
        Assert.assertTrue(childTask.current(exactEndTimeStamp));

        LocalTaskHierarchy parentLocalTaskHierarchy = getParentTaskHierarchy(childTask, exactEndTimeStamp);
        if (parentLocalTaskHierarchy != null) {
            Assert.assertTrue(parentLocalTaskHierarchy.current(exactEndTimeStamp));
            parentLocalTaskHierarchy.setEndExactTimeStamp(exactEndTimeStamp);
        }
    }

    @NonNull
    public CustomTime getCustomTime(int customTimeId) {
        Assert.assertTrue(mCustomTimes.containsKey(customTimeId));

        CustomTime customTime = mCustomTimes.get(customTimeId);
        Assert.assertTrue(customTime != null);

        return customTime;
    }

    private List<CustomTime> getCurrentCustomTimes() {
        return Stream.of(mCustomTimes.values())
                .filter(CustomTime::getCurrent)
                .collect(Collectors.toList());
    }

    @NonNull
    private HashMap<InstanceKey, GroupListLoader.InstanceData> getChildInstanceDatas(@NonNull MergedInstance instance, @NonNull ExactTimeStamp now, @NonNull GroupListLoader.InstanceDataParent instanceDataParent) {
        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();

        for (MergedInstance childInstance : instance.getChildInstances(now)) {
            MergedTask childTask = getTask(childInstance.getTaskKey());

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), instanceDataParent, childInstance.getInstanceDateTime().getTime().getTimePair(), childTask.getNote(), childTask.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(childInstance, now, instanceData));
            instanceDatas.put(childInstance.getInstanceKey(), instanceData);
        }

        return instanceDatas;
    }

    @NonNull
    private Map<TaskKey, CreateTaskLoader.TaskTreeData> getChildTaskDatas(@NonNull ExactTimeStamp now, @NonNull MergedTask parentTask, @NonNull Context context, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(parentTask.getChildTasks(now))
                .filterNot(childTask -> excludedTaskKeys.contains(childTask.getTaskKey()))
                .collect(Collectors.toMap(MergedTask::getTaskKey, childTask -> new CreateTaskLoader.TaskTreeData(childTask.getName(), getChildTaskDatas(now, childTask, context, excludedTaskKeys), childTask.getTaskKey(), childTask.getScheduleText(context, now), childTask.getNote(), childTask.getStartExactTimeStamp())));
    }

    @NonNull
    private Map<TaskKey, CreateTaskLoader.TaskTreeData> getTaskDatas(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(getTasks().values())
                .filter(task -> task.current(now))
                .filter(task -> task.isVisible(now))
                .filter(task -> task.isRootTask(now))
                .filterNot(task -> excludedTaskKeys.contains(task.getTaskKey()))
                .collect(Collectors.toMap(MergedTask::getTaskKey, task -> new CreateTaskLoader.TaskTreeData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskKeys), task.getTaskKey(), task.getScheduleText(context, now), task.getNote(), task.getStartExactTimeStamp())));
    }

    // notifications

    @SuppressLint("NewApi")
    private void setExact(@NonNull Context context, long time, @NonNull PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }

    private void notifyInstance(@NonNull Context context, @NonNull MergedInstance instance, boolean silent, @NonNull ExactTimeStamp now) {
        MergedTask task = instance.getTask();
        int notificationId = instance.getNotificationId();
        InstanceKey instanceKey = instance.getInstanceKey();

        Intent deleteIntent = InstanceNotificationDeleteService.getIntent(context, instanceKey);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(context, notificationId, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent contentIntent = ShowInstanceActivity.getNotificationIntent(context, instanceKey);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        ArrayList<NotificationCompat.Action> actions = new ArrayList<>();

        Intent doneIntent = InstanceDoneService.getIntent(context, instanceKey, notificationId);
        PendingIntent pendingDoneIntent = PendingIntent.getService(context, notificationId, doneIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        actions.add(new NotificationCompat.Action.Builder(R.drawable.ic_done_white_24dp, context.getString(R.string.done), pendingDoneIntent).build());

        Intent hourIntent = InstanceHourService.getIntent(context, instanceKey, notificationId);
        PendingIntent pendingHourIntent = PendingIntent.getService(context, notificationId, hourIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        actions.add(new NotificationCompat.Action.Builder(R.drawable.ic_alarm_white_24dp, context.getString(R.string.hour), pendingHourIntent).build());

        List<MergedInstance> childInstances = instance.getChildInstances(now);

        String text;
        NotificationCompat.Style style;
        if (!childInstances.isEmpty()) {
            Stream<MergedInstance> notDone = Stream.of(childInstances)
                    .filter(childInstance -> childInstance.getDone() == null)
                    .sortBy(childInstance -> childInstance.getTask().getStartExactTimeStamp());

            //noinspection ConstantConditions
            Stream<MergedInstance> done = Stream.of(childInstances)
                    .filter(childInstance -> childInstance.getDone() != null)
                    .sortBy(childInstance -> -childInstance.getDone().getLong());

            List<String> children = Stream.concat(notDone, done)
                    .map(MergedInstance::getName)
                    .collect(Collectors.toList());

            text = TextUtils.join(", ", children);
            style = getInboxStyle(context, children);
        } else if (!TextUtils.isEmpty(task.getNote())) {
            text = task.getNote();

            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.bigText(task.getNote());

            style = bigTextStyle;
        } else {
            text = null;
            style = null;
        }

        notify(context, instance.getName(), text, notificationId, pendingDeleteIntent, pendingContentIntent, silent, actions, instance.getInstanceDateTime().getTimeStamp().getLong(), style, true);
    }

    private void notifyGroup(@NonNull Context context, @NonNull Collection<MergedInstance> instances, boolean silent, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(instances.size() > TickService.MAX_NOTIFICATIONS);

        ArrayList<String> names = new ArrayList<>();
        ArrayList<InstanceKey> instanceKeys = new ArrayList<>();
        for (MergedInstance instance : instances) {
            names.add(instance.getName());
            instanceKeys.add(instance.getInstanceKey());
        }

        Intent deleteIntent = GroupNotificationDeleteService.getIntent(context, instanceKeys);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent contentIntent = ShowNotificationGroupActivity.getIntent(context, instanceKeys);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.InboxStyle inboxStyle = getInboxStyle(context, Stream.of(instances)
                .sorted((lhs, rhs) -> {
                    int timeStampComparison = lhs.getInstanceDateTime().getTimeStamp().compareTo(rhs.getInstanceDateTime().getTimeStamp());
                    if (timeStampComparison != 0)
                        return timeStampComparison;

                    return lhs.getTask().getStartExactTimeStamp().compareTo(rhs.getTask().getStartExactTimeStamp());
                })
                .map(notificationInstanceData -> notificationInstanceData.getName() + " (" + notificationInstanceData.getDisplayText(context, now) + ")")
                .collect(Collectors.toList()));

        notify(context, instances.size() + " " + context.getString(R.string.multiple_reminders), TextUtils.join(", ", names), 0, pendingDeleteIntent, pendingContentIntent, silent, new ArrayList<>(), null, inboxStyle, false);
    }

    @NonNull
    private NotificationCompat.InboxStyle getInboxStyle(@NonNull Context context, @NonNull List<String> lines) {
        Assert.assertTrue(!lines.isEmpty());

        int max = 5;

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        Stream.of(lines)
                .limit(max)
                .forEach(inboxStyle::addLine);

        int extraCount = lines.size() - max;

        if (extraCount > 0)
            inboxStyle.setSummaryText("+" + extraCount + " " + context.getString(R.string.more));

        return inboxStyle;
    }

    private void notify(@NonNull Context context, @NonNull String title, @Nullable String text, int notificationId, @NonNull PendingIntent deleteIntent, @NonNull PendingIntent contentIntent, boolean silent, @NonNull List<NotificationCompat.Action> actions, @Nullable Long when, @Nullable NotificationCompat.Style style, boolean autoCancel) {
        Assert.assertTrue(!TextUtils.isEmpty(title));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = (new NotificationCompat.Builder(context))
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ikona_bez)
                .setDeleteIntent(deleteIntent)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (!TextUtils.isEmpty(text))
            builder.setContentText(text);

        if (!silent)
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

        Assert.assertTrue(actions.size() <= 3);

        Stream.of(actions)
                .forEach(builder::addAction);

        if (when != null)
            builder.setWhen(when);

        if (style != null)
            builder.setStyle(style);

        if (autoCancel)
            builder.setAutoCancel(true);

        Notification notification = builder.build();

        if (!silent)
            notification.defaults |= Notification.DEFAULT_VIBRATE;

        notificationManager.notify(notificationId, notification);
    }

    public static class Irrelevant {
        @NonNull
        public final List<CustomTime> mCustomTimes; // todo customTimes

        @NonNull
        public final List<MergedTask> mTasks;

        @NonNull
        public final List<MergedInstance> mInstances;

        Irrelevant(@NonNull List<CustomTime> customTimes, @NonNull List<MergedTask> tasks, @NonNull List<MergedInstance> instances) {
            mCustomTimes = customTimes;
            mTasks = tasks;
            mInstances = instances;
        }
    }

    private class TaskRelevance {
        @NonNull
        private final MergedTask mTask;
        private boolean mRelevant = false;

        TaskRelevance(@NonNull MergedTask task) {
            mTask = task;
        }

        void setRelevant(@NonNull Map<TaskKey, TaskRelevance> taskRelevances, @NonNull Map<InstanceKey, InstanceRelevance> instanceRelevances, @NonNull Map<Integer, CustomTimeRelevance> customTimeRelevances, @NonNull ExactTimeStamp now) {
            if (mRelevant)
                return;

            mRelevant = true;

            TaskKey taskKey = mTask.getTaskKey();

            // mark parents relevant
            Stream.of(mTaskHierarchies.values())
                    .filter(taskHierarchy -> taskHierarchy.getChildTaskKey().equals(taskKey))
                    .map(TaskHierarchy::getParentTaskKey)
                    .map(taskRelevances::get)
                    .forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            // mark children relevant
            Stream.of(mTaskHierarchies.values())
                    .filter(taskHierarchy -> taskHierarchy.getParentTaskKey().equals(taskKey))
                    .map(TaskHierarchy::getChildTaskKey)
                    .map(taskRelevances::get)
                    .forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            Date oldestVisible = mTask.getOldestVisible();
            Assert.assertTrue(oldestVisible != null);

            // mark instances relevant
            Stream.of(getPastInstances(mTask, now))
                    .filter(instance -> instance.getScheduleDateTime().getDate().compareTo(oldestVisible) >= 0)
                    .map(instance -> {
                        InstanceKey instanceKey = instance.getInstanceKey();

                        if (!instanceRelevances.containsKey(instanceKey))
                            instanceRelevances.put(instanceKey, new InstanceRelevance(instance));

                        return instanceRelevances.get(instanceKey);
                    })
                    .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            Stream.of(getExistingInstances(mTask))
                    .filter(instance -> instance.getScheduleDateTime().getDate().compareTo(oldestVisible) >= 0)
                    .map(MergedInstance::getInstanceKey)
                    .map(instanceRelevances::get)
                    .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            // mark custom times relevant
            if (mTask.current(now))
                Stream.of(mTask.getCurrentSchedules(now))
                        .map(MergedSchedule::getCustomTimeId)
                        .filter(customTimeId -> customTimeId != null)
                        .map(customTimeRelevances::get)
                        .forEach(CustomTimeRelevance::setRelevant);
        }

        boolean getRelevant() {
            return mRelevant;
        }

        public MergedTask getTask() {
            return mTask;
        }
    }

    private static class InstanceRelevance {
        private final MergedInstance mInstance;
        private boolean mRelevant = false;

        InstanceRelevance(@NonNull MergedInstance instance) {
            mInstance = instance;
        }

        void setRelevant(@NonNull Map<TaskKey, TaskRelevance> taskRelevances, @NonNull Map<InstanceKey, InstanceRelevance> instanceRelevances, @NonNull Map<Integer, CustomTimeRelevance> customTimeRelevances, @NonNull ExactTimeStamp now) {
            if (mRelevant)
                return;

            mRelevant = true;

            // set task relevant
            TaskRelevance taskRelevance = taskRelevances.get(mInstance.getTaskKey());
            Assert.assertTrue(taskRelevance != null);

            taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now);

            // set parent instance relevant
            if (!mInstance.isRootInstance(now)) {
                MergedInstance parentInstance = mInstance.getParentInstance(now);
                Assert.assertTrue(parentInstance != null);

                InstanceKey parentInstanceKey = parentInstance.getInstanceKey();

                if (!instanceRelevances.containsKey(parentInstanceKey))
                    instanceRelevances.put(parentInstanceKey, new InstanceRelevance(parentInstance));

                InstanceRelevance parentInstanceRelevance = instanceRelevances.get(parentInstanceKey);
                Assert.assertTrue(parentInstanceRelevance != null);

                parentInstanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now);
            }

            // set child instances relevant
            Stream.of(mInstance.getChildInstances(now))
                    .map(instance -> {
                        InstanceKey instanceKey = instance.getInstanceKey();

                        if (!instanceRelevances.containsKey(instanceKey))
                            instanceRelevances.put(instanceKey, new InstanceRelevance(instance));

                        return instanceRelevances.get(instanceKey);
                    })
                    .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            // set custom time relevant
            Integer scheduleCustomTimeId = mInstance.getScheduleTimePair().mCustomTimeId;
            if (scheduleCustomTimeId != null) {
                CustomTimeRelevance customTimeRelevance = customTimeRelevances.get(scheduleCustomTimeId);
                Assert.assertTrue(customTimeRelevance != null);

                customTimeRelevance.setRelevant();
            }

            // set custom time relevant
            Integer instanceCustomTimeId = mInstance.getInstanceTimePair().mCustomTimeId;
            if (instanceCustomTimeId != null) {
                CustomTimeRelevance customTimeRelevance = customTimeRelevances.get(instanceCustomTimeId);
                Assert.assertTrue(customTimeRelevance != null);

                customTimeRelevance.setRelevant();
            }
        }

        boolean getRelevant() {
            return mRelevant;
        }

        public MergedInstance getInstance() {
            return mInstance;
        }
    }

    private static class CustomTimeRelevance {
        private final CustomTime mCustomTime;
        private boolean mRelevant = false;

        CustomTimeRelevance(@NonNull CustomTime customTime) {
            mCustomTime = customTime;
        }

        void setRelevant() {
            mRelevant = true;
        }

        boolean getRelevant() {
            return mRelevant;
        }

        CustomTime getCustomTime() {
            return mCustomTime;
        }
    }
}
