package com.krystianwsul.checkme.domainmodel.local;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DailySchedule;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MonthlyDaySchedule;
import com.krystianwsul.checkme.domainmodel.MonthlyWeekSchedule;
import com.krystianwsul.checkme.domainmodel.Schedule;
import com.krystianwsul.checkme.domainmodel.SingleSchedule;
import com.krystianwsul.checkme.domainmodel.WeeklySchedule;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
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
import com.krystianwsul.checkme.utils.InstanceMap;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskHierarchyContainer;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressLint("UseSparseArrays")
public class LocalFactory {
    private static LocalFactory sLocalFactory;

    @NonNull
    private final PersistenceManger mPersistenceManager;

    @NonNull
    private final HashMap<Integer, LocalCustomTime> mLocalCustomTimes = new HashMap<>();

    @NonNull
    private final HashMap<Integer, LocalTask> mLocalTasks = new HashMap<>();

    @NonNull
    private final TaskHierarchyContainer<Integer, LocalTaskHierarchy> mLocalTaskHierarchies = new TaskHierarchyContainer<>();

    @NonNull
    private final InstanceMap<LocalInstance> mExistingLocalInstances = new InstanceMap<>();

    public static LocalFactory getInstance(@NonNull Context context) {
        if (sLocalFactory == null)
            sLocalFactory = new LocalFactory(context);

        return sLocalFactory;
    }

    private LocalFactory(@NonNull Context context) {
        mPersistenceManager = PersistenceManger.getInstance(context);
    }

    public LocalFactory(@NonNull PersistenceManger persistenceManger) {
        mPersistenceManager = persistenceManger;
    }

    public void reset() {
        sLocalFactory = null;

        mPersistenceManager.reset();
    }

    public void initialize(@NonNull DomainFactory domainFactory) {
        Collection<CustomTimeRecord> customTimeRecords = mPersistenceManager.getCustomTimeRecords();
        Assert.assertTrue(customTimeRecords != null);

        for (CustomTimeRecord customTimeRecord : customTimeRecords) {
            Assert.assertTrue(customTimeRecord != null);

            LocalCustomTime localCustomTime = new LocalCustomTime(domainFactory, customTimeRecord);
            mLocalCustomTimes.put(localCustomTime.getId(), localCustomTime);
        }

        Collection<TaskRecord> taskRecords = mPersistenceManager.getTaskRecords();
        Assert.assertTrue(taskRecords != null);

        for (TaskRecord taskRecord : taskRecords) {
            Assert.assertTrue(taskRecord != null);

            LocalTask localTask = new LocalTask(domainFactory, taskRecord);

            List<Schedule> schedules = loadSchedules(domainFactory, taskRecord.getId());

            localTask.addSchedules(schedules);

            Assert.assertTrue(!mLocalTasks.containsKey(localTask.getId()));
            mLocalTasks.put(localTask.getId(), localTask);
        }

        Collection<TaskHierarchyRecord> taskHierarchyRecords = mPersistenceManager.getTaskHierarchyRecords();
        Assert.assertTrue(taskHierarchyRecords != null);

        for (TaskHierarchyRecord taskHierarchyRecord : taskHierarchyRecords) {
            Assert.assertTrue(taskHierarchyRecord != null);

            LocalTask parentLocalTask = mLocalTasks.get(taskHierarchyRecord.getParentTaskId());
            Assert.assertTrue(parentLocalTask != null);

            LocalTask childLocalTask = mLocalTasks.get(taskHierarchyRecord.getChildTaskId());
            Assert.assertTrue(childLocalTask != null);

            LocalTaskHierarchy localTaskHierarchy = new LocalTaskHierarchy(domainFactory, taskHierarchyRecord);

            mLocalTaskHierarchies.add(localTaskHierarchy.getId(), localTaskHierarchy);
        }

        Collection<InstanceRecord> instanceRecords = mPersistenceManager.getInstanceRecords();
        Assert.assertTrue(instanceRecords != null);

        for (InstanceRecord instanceRecord : instanceRecords) {
            LocalTask localTask = mLocalTasks.get(instanceRecord.getTaskId());
            Assert.assertTrue(localTask != null);

            LocalInstance localInstance = new LocalInstance(domainFactory, instanceRecord);
            mExistingLocalInstances.add(localInstance);
        }
    }

    @NonNull
    private List<Schedule> loadSchedules(@NonNull DomainFactory domainFactory, int localTaskId) {
        List<ScheduleRecord> scheduleRecords = mPersistenceManager.getScheduleRecords(localTaskId);
        Assert.assertTrue(scheduleRecords != null);

        ArrayList<Schedule> schedules = new ArrayList<>();

        for (ScheduleRecord scheduleRecord : scheduleRecords) {
            Assert.assertTrue(scheduleRecord.getType() >= 0);
            Assert.assertTrue(scheduleRecord.getType() < ScheduleType.values().length);

            ScheduleType scheduleType = ScheduleType.values()[scheduleRecord.getType()];

            switch (scheduleType) {
                case SINGLE:
                    schedules.add(loadSingleSchedule(domainFactory, scheduleRecord));
                    break;
                case DAILY:
                    schedules.add(loadDailySchedule(domainFactory, scheduleRecord));
                    break;
                case WEEKLY:
                    schedules.add(loadWeeklySchedule(domainFactory, scheduleRecord));
                    break;
                case MONTHLY_DAY:
                    schedules.add(loadMonthlyDaySchedule(domainFactory, scheduleRecord));
                    break;
                case MONTHLY_WEEK:
                    schedules.add(loadMonthlyWeekSchedule(domainFactory, scheduleRecord));
                    break;
                default:
                    throw new IndexOutOfBoundsException("unknown schedule type");
            }
        }

        return schedules;
    }

    @NonNull
    private Schedule loadSingleSchedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord) {
        SingleScheduleRecord singleScheduleRecord = mPersistenceManager.getSingleScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(singleScheduleRecord != null);

        return new SingleSchedule(domainFactory, new LocalSingleScheduleBridge(scheduleRecord, singleScheduleRecord));
    }

    @NonNull
    private DailySchedule loadDailySchedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord) {
        DailyScheduleRecord dailyScheduleRecord = mPersistenceManager.getDailyScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(dailyScheduleRecord != null);

        return new DailySchedule(domainFactory, new LocalDailyScheduleBridge(scheduleRecord, dailyScheduleRecord));
    }

    @NonNull
    private WeeklySchedule loadWeeklySchedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord) {
        WeeklyScheduleRecord weeklyScheduleRecord = mPersistenceManager.getWeeklyScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(weeklyScheduleRecord != null);

        return new WeeklySchedule(domainFactory, new LocalWeeklyScheduleBridge(scheduleRecord, weeklyScheduleRecord));
    }

    @NonNull
    private MonthlyDaySchedule loadMonthlyDaySchedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord) {
        MonthlyDayScheduleRecord monthlyDayScheduleRecord = mPersistenceManager.getMonthlyDayScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(monthlyDayScheduleRecord != null);

        return new MonthlyDaySchedule(domainFactory, new LocalMonthlyDayScheduleBridge(scheduleRecord, monthlyDayScheduleRecord));
    }

    @NonNull
    private MonthlyWeekSchedule loadMonthlyWeekSchedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord) {
        MonthlyWeekScheduleRecord monthlyWeekScheduleRecord = mPersistenceManager.getMonthlyWeekScheduleRecord(scheduleRecord.getId());
        Assert.assertTrue(monthlyWeekScheduleRecord != null);

        return new MonthlyWeekSchedule(domainFactory, new LocalMonthlyWeekScheduleBridge(scheduleRecord, monthlyWeekScheduleRecord));
    }

    public void save(@NonNull Context context) {
        mPersistenceManager.save(context);
    }

    @Nullable
    public InstanceShownRecord getInstanceShownRecord(@NonNull String taskId, int scheduleYear, int scheduleMonth, int scheduleDay, @Nullable String scheduleCustomTimeId, @Nullable Integer scheduleHour, @Nullable Integer scheduleMinute) {
        List<InstanceShownRecord> matches;
        if (scheduleCustomTimeId != null) {
            Assert.assertTrue(scheduleHour == null);
            Assert.assertTrue(scheduleMinute == null);

            matches = Stream.of(mPersistenceManager.getInstanceShownRecords())
                    .filter(instanceShownRecord -> instanceShownRecord.getTaskId().equals(taskId))
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleYear() == scheduleYear)
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleMonth() == scheduleMonth)
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleDay() == scheduleDay)
                    .filter(instanceShownRecord -> scheduleCustomTimeId.equals(instanceShownRecord.getScheduleCustomTimeId()))
                    .collect(Collectors.toList());
        } else {
            Assert.assertTrue(scheduleHour != null);
            Assert.assertTrue(scheduleMinute != null);

            matches = Stream.of(mPersistenceManager.getInstanceShownRecords())
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
    public InstanceShownRecord createInstanceShownRecord(@NonNull DomainFactory domainFactory, @NonNull String remoteTaskId, @NonNull DateTime scheduleDateTime, @NonNull String projectId) {
        TimePair timePair = scheduleDateTime.getTime().getTimePair();

        String remoteCustomTimeId;
        Integer hour;
        Integer minute;
        if (timePair.mHourMinute != null) {
            Assert.assertTrue(timePair.mCustomTimeKey == null);

            remoteCustomTimeId = null;

            hour = timePair.mHourMinute.getHour();
            minute = timePair.mHourMinute.getMinute();
        } else {
            Assert.assertTrue(timePair.mCustomTimeKey != null);

            remoteCustomTimeId = domainFactory.getRemoteCustomTimeId(timePair.mCustomTimeKey);

            hour = null;
            minute = null;
        }

        return mPersistenceManager.createInstanceShownRecord(remoteTaskId, scheduleDateTime.getDate(), remoteCustomTimeId, hour, minute, projectId);
    }

    void deleteTask(@NonNull LocalTask localTask) {
        Assert.assertTrue(mLocalTasks.containsKey(localTask.getId()));

        mLocalTasks.remove(localTask.getId());
    }

    void deleteTaskHierarchy(@NonNull LocalTaskHierarchy localTaskHierarchy) {
        mLocalTaskHierarchies.removeForce(localTaskHierarchy.getId());
    }

    void deleteInstance(@NonNull LocalInstance localInstance) {
        mExistingLocalInstances.removeForce(localInstance);
    }

    void deleteCustomTime(@NonNull LocalCustomTime localCustomTime) {
        Assert.assertTrue(mLocalCustomTimes.containsKey(localCustomTime.getId()));

        mLocalCustomTimes.remove(localCustomTime.getId());
    }

    public void deleteInstanceShownRecords(@NonNull Set<TaskKey> taskKeys) {
        mPersistenceManager.deleteInstanceShownRecords(taskKeys);
    }

    @NonNull
    public LocalTask createScheduleRootTask(@NonNull DomainFactory domainFactory, @NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        LocalTask rootLocalTask = createLocalTaskHelper(domainFactory, name, now, note);

        List<Schedule> schedules = createSchedules(domainFactory, rootLocalTask, scheduleDatas, now);
        Assert.assertTrue(!schedules.isEmpty());

        rootLocalTask.addSchedules(schedules);

        return rootLocalTask;
    }

    @NonNull
    public LocalTask createLocalTaskHelper(@NonNull DomainFactory domainFactory, @NonNull String name, @NonNull ExactTimeStamp now, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        TaskRecord taskRecord = mPersistenceManager.createTaskRecord(name, now, note);

        LocalTask rootLocalTask = new LocalTask(domainFactory, taskRecord);

        Assert.assertTrue(!mLocalTasks.containsKey(rootLocalTask.getId()));
        mLocalTasks.put(rootLocalTask.getId(), rootLocalTask);

        return rootLocalTask;
    }

    @NonNull
    List<Schedule> createSchedules(@NonNull DomainFactory domainFactory, @NonNull LocalTask rootLocalTask, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(rootLocalTask.current(startExactTimeStamp));

        List<Schedule> schedules = new ArrayList<>();

        for (CreateTaskLoader.ScheduleData scheduleData : scheduleDatas) {
            Assert.assertTrue(scheduleData != null);

            switch (scheduleData.getScheduleType()) {
                case SINGLE: {
                    CreateTaskLoader.SingleScheduleData singleScheduleData = (CreateTaskLoader.SingleScheduleData) scheduleData;

                    Date date = singleScheduleData.Date;
                    Time time = domainFactory.getTime(singleScheduleData.TimePair);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.SINGLE, startExactTimeStamp);

                    SingleScheduleRecord singleScheduleRecord = mPersistenceManager.createSingleScheduleRecord(scheduleRecord.getId(), date, time);

                    schedules.add(new SingleSchedule(domainFactory, new LocalSingleScheduleBridge(scheduleRecord, singleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                    Time time = domainFactory.getTime(dailyScheduleData.TimePair);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.DAILY, startExactTimeStamp);

                    DailyScheduleRecord dailyScheduleRecord = mPersistenceManager.createDailyScheduleRecord(scheduleRecord.getId(), time);

                    schedules.add(new DailySchedule(domainFactory, new LocalDailyScheduleBridge(scheduleRecord, dailyScheduleRecord)));
                    break;
                }
                case WEEKLY: {
                    CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;

                    DayOfWeek dayOfWeek = weeklyScheduleData.DayOfWeek;
                    Time time = domainFactory.getTime(weeklyScheduleData.TimePair);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.WEEKLY, startExactTimeStamp);

                    WeeklyScheduleRecord weeklyScheduleRecord = mPersistenceManager.createWeeklyScheduleRecord(scheduleRecord.getId(), dayOfWeek, time);

                    schedules.add(new WeeklySchedule(domainFactory, new LocalWeeklyScheduleBridge(scheduleRecord, weeklyScheduleRecord)));
                    break;
                }
                case MONTHLY_DAY: {
                    CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_DAY, startExactTimeStamp);

                    MonthlyDayScheduleRecord monthlyDayScheduleRecord = mPersistenceManager.createMonthlyDayScheduleRecord(scheduleRecord.getId(), monthlyDayScheduleData.mDayOfMonth, monthlyDayScheduleData.mBeginningOfMonth, domainFactory.getTime(monthlyDayScheduleData.TimePair));

                    schedules.add(new MonthlyDaySchedule(domainFactory, new LocalMonthlyDayScheduleBridge(scheduleRecord, monthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_WEEK, startExactTimeStamp);

                    MonthlyWeekScheduleRecord monthlyWeekScheduleRecord = mPersistenceManager.createMonthlyWeekScheduleRecord(scheduleRecord.getId(), monthlyWeekScheduleData.mDayOfMonth, monthlyWeekScheduleData.mDayOfWeek, monthlyWeekScheduleData.mBeginningOfMonth, domainFactory.getTime(monthlyWeekScheduleData.TimePair));

                    schedules.add(new MonthlyWeekSchedule(domainFactory, new LocalMonthlyWeekScheduleBridge(scheduleRecord, monthlyWeekScheduleRecord)));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        return schedules;
    }

    void createTaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull LocalTask parentLocalTask, @NonNull LocalTask childLocalTask, @NonNull ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(parentLocalTask.current(startExactTimeStamp));
        Assert.assertTrue(childLocalTask.current(startExactTimeStamp));

        TaskHierarchyRecord taskHierarchyRecord = mPersistenceManager.createTaskHierarchyRecord(parentLocalTask, childLocalTask, startExactTimeStamp);
        Assert.assertTrue(taskHierarchyRecord != null);

        LocalTaskHierarchy localTaskHierarchy = new LocalTaskHierarchy(domainFactory, taskHierarchyRecord);
        mLocalTaskHierarchies.add(localTaskHierarchy.getId(), localTaskHierarchy);
    }

    @NonNull
    LocalTask createChildTask(@NonNull DomainFactory domainFactory, @NonNull ExactTimeStamp now, @NonNull LocalTask parentTask, @NonNull String name, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(parentTask.current(now));

        LocalTask childLocalTask = createLocalTaskHelper(domainFactory, name, now, note);

        createTaskHierarchy(domainFactory, parentTask, childLocalTask, now);

        return childLocalTask;
    }

    @NonNull
    InstanceRecord createInstanceRecord(@NonNull LocalTask localTask, @NonNull LocalInstance localInstance, @NonNull DateTime scheduleDateTime, @NonNull ExactTimeStamp now) {
        mExistingLocalInstances.add(localInstance);

        return mPersistenceManager.createInstanceRecord(localTask, scheduleDateTime, now);
    }

    @NonNull
    public List<InstanceShownRecord> getInstanceShownRecords() {
        return mPersistenceManager.getInstanceShownRecords();
    }

    @NonNull
    public Collection<LocalTask> getTasks() {
        return mLocalTasks.values();
    }

    public void convertLocalToRemoteHelper(@NonNull DomainFactory.LocalToRemoteConversion localToRemoteConversion, @NonNull LocalTask localTask, @NonNull Set<String> recordOf) {
        if (localToRemoteConversion.mLocalTasks.containsKey(localTask.getId()))
            return;

        TaskKey taskKey = localTask.getTaskKey();

        localToRemoteConversion.mLocalTasks.put(localTask.getId(), Pair.create(localTask, mExistingLocalInstances.get(taskKey).values()));

        Set<LocalTaskHierarchy> parentLocalTaskHierarchies = mLocalTaskHierarchies.getByChildTaskKey(taskKey);

        localToRemoteConversion.mLocalTaskHierarchies.addAll(parentLocalTaskHierarchies);

        Stream.of(mLocalTaskHierarchies.getByParentTaskKey(taskKey))
                .map(LocalTaskHierarchy::getChildTask)
                .forEach(childTask -> convertLocalToRemoteHelper(localToRemoteConversion, childTask, recordOf));

        Stream.of(parentLocalTaskHierarchies)
                .map(LocalTaskHierarchy::getParentTask)
                .forEach(parentTask -> convertLocalToRemoteHelper(localToRemoteConversion, parentTask, recordOf));
    }

    /*
    @NonNull
    public Collection<LocalTaskHierarchy> getTaskHierarchies() {
        return mLocalTaskHierarchies.values();
    }
    */

    @NonNull
    public LocalCustomTime createLocalCustomTime(@NonNull DomainFactory domainFactory, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
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

        LocalCustomTime localCustomTime = new LocalCustomTime(domainFactory, customTimeRecord);
        Assert.assertTrue(!mLocalCustomTimes.containsKey(localCustomTime.getId()));

        mLocalCustomTimes.put(localCustomTime.getId(), localCustomTime);

        return localCustomTime;
    }

    @NonNull
    public LocalCustomTime getLocalCustomTime(int localCustomTimeId) {
        Assert.assertTrue(mLocalCustomTimes.containsKey(localCustomTimeId));

        LocalCustomTime localCustomTime = mLocalCustomTimes.get(localCustomTimeId);
        Assert.assertTrue(localCustomTime != null);

        return localCustomTime;
    }

    @NonNull
    public Collection<LocalCustomTime> getLocalCustomTimes() {
        return mLocalCustomTimes.values();
    }

    public void clearRemoteCustomTimeRecords() {
        Stream.of(mLocalCustomTimes.values())
                .forEach(LocalCustomTime::clearRemoteRecords);
    }

    @NonNull
    public List<LocalCustomTime> getCurrentCustomTimes() {
        return Stream.of(mLocalCustomTimes.values())
                .filter(LocalCustomTime::getCurrent)
                .collect(Collectors.toList());
    }

    @Nullable
    public LocalCustomTime getLocalCustomTime(@NonNull String remoteCustomTimeId) {
        List<LocalCustomTime> matches = Stream.of(mLocalCustomTimes.values())
                .filter(localCustomTime -> localCustomTime.hasRemoteRecord() && localCustomTime.getRemoteId().equals(remoteCustomTimeId))
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            return null;
        } else {
            Assert.assertTrue(matches.size() == 1);

            return matches.get(0);
        }

    }

    public boolean hasLocalCustomTime(int localCustomTimeId) {
        return mLocalCustomTimes.containsKey(localCustomTimeId);
    }

    public int getInstanceCount() {
        return mExistingLocalInstances.size();
    }

    @NonNull
    Map<ScheduleKey, LocalInstance> getExistingInstances(@NonNull TaskKey taskKey) {
        return mExistingLocalInstances.get(taskKey);
    }

    @NonNull
    public List<LocalInstance> getExistingInstances() {
        return mExistingLocalInstances.values();
    }

    @Nullable
    public LocalInstance getExistingInstanceIfPresent(@NonNull InstanceKey instanceKey) {
        return mExistingLocalInstances.getIfPresent(instanceKey);
    }

    @NonNull
    public LocalTask getTaskForce(int taskId) {
        LocalTask localTask = mLocalTasks.get(taskId);
        Assert.assertTrue(localTask != null);

        return localTask;
    }

    @Nullable
    public LocalTask getTaskIfPresent(int taskId) {
        return mLocalTasks.get(taskId);
    }

    @NonNull
    public Set<Integer> getTaskIds() {
        return mLocalTasks.keySet();
    }

    public int getTaskCount() {
        return mLocalTasks.size();
    }

    @NonNull
    Set<LocalTaskHierarchy> getTaskHierarchiesByChildTaskKey(@NonNull TaskKey childTaskKey) {
        return mLocalTaskHierarchies.getByChildTaskKey(childTaskKey);
    }

    @NonNull
    Set<LocalTaskHierarchy> getTaskHierarchiesByParentTaskKey(@NonNull TaskKey parentTaskKey) {
        return mLocalTaskHierarchies.getByParentTaskKey(parentTaskKey);
    }

    @NonNull
    public String getUuid() {
        return mPersistenceManager.getUuid();
    }
}
