package com.krystianwsul.checkme.domainmodel.local;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.CustomTime;
import com.krystianwsul.checkme.domainmodel.DailySchedule;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.LocalDailyScheduleBridge;
import com.krystianwsul.checkme.domainmodel.LocalInstance;
import com.krystianwsul.checkme.domainmodel.LocalMonthlyDayScheduleBridge;
import com.krystianwsul.checkme.domainmodel.LocalMonthlyWeekScheduleBridge;
import com.krystianwsul.checkme.domainmodel.LocalSingleScheduleBridge;
import com.krystianwsul.checkme.domainmodel.LocalTask;
import com.krystianwsul.checkme.domainmodel.LocalTaskHierarchy;
import com.krystianwsul.checkme.domainmodel.LocalWeeklyScheduleBridge;
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
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@SuppressLint("UseSparseArrays")
public class LocalFactory {
    private static LocalFactory sLocalFactory;

    @NonNull
    public final PersistenceManger mPersistenceManager;

    @NonNull
    public final HashMap<Integer, CustomTime> mLocalCustomTimes = new HashMap<>();

    @NonNull
    public final HashMap<Integer, LocalTask> mLocalTasks = new HashMap<>();

    @NonNull
    public final HashMap<Integer, LocalTaskHierarchy> mLocalTaskHierarchies = new HashMap<>();

    @NonNull
    public final ArrayList<LocalInstance> mExistingLocalInstances = new ArrayList<>();

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

            CustomTime customTime = new CustomTime(customTimeRecord);
            mLocalCustomTimes.put(customTime.getId(), customTime);
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

            Assert.assertTrue(!mLocalTaskHierarchies.containsKey(localTaskHierarchy.getId()));
            mLocalTaskHierarchies.put(localTaskHierarchy.getId(), localTaskHierarchy);
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
    public InstanceShownRecord createInstanceShownRecord(@NonNull String remoteTaskId, @NonNull DateTime scheduleDateTime) {
        return mPersistenceManager.createInstanceShownRecord(remoteTaskId, scheduleDateTime);
    }

    public void deleteTask(@NonNull LocalTask localTask) {
        Assert.assertTrue(mLocalTasks.containsKey(localTask.getId()));

        mLocalTasks.remove(localTask.getId());
    }

    public void deleteTaskHierarchy(@NonNull LocalTaskHierarchy localTasHierarchy) {
        Assert.assertTrue(mLocalTaskHierarchies.containsKey(localTasHierarchy.getId()));

        mLocalTaskHierarchies.remove(localTasHierarchy.getId());
    }

    public void deleteInstance(@NonNull LocalInstance localInstance) {
        Assert.assertTrue(mExistingLocalInstances.contains(localInstance));

        mExistingLocalInstances.remove(localInstance);
    }

    public void deleteInstanceShownRecords(@NonNull Set<String> taskIds) {
        mPersistenceManager.deleteInstanceShownRecords(taskIds);
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
    public List<Schedule> createSchedules(@NonNull DomainFactory domainFactory, @NonNull LocalTask rootLocalTask, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull ExactTimeStamp startExactTimeStamp) {
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(rootLocalTask.current(startExactTimeStamp));

        List<Schedule> schedules = new ArrayList<>();

        for (CreateTaskLoader.ScheduleData scheduleData : scheduleDatas) {
            Assert.assertTrue(scheduleData != null);

            switch (scheduleData.getScheduleType()) {
                case SINGLE: {
                    CreateTaskLoader.SingleScheduleData singleScheduleData = (CreateTaskLoader.SingleScheduleData) scheduleData;

                    Date date = singleScheduleData.Date;
                    Time time = getTime(singleScheduleData.TimePair);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.SINGLE, startExactTimeStamp);

                    SingleScheduleRecord singleScheduleRecord = mPersistenceManager.createSingleScheduleRecord(scheduleRecord.getId(), date, time);

                    schedules.add(new SingleSchedule(domainFactory, new LocalSingleScheduleBridge(scheduleRecord, singleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                    Time time = getTime(dailyScheduleData.TimePair);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.DAILY, startExactTimeStamp);

                    DailyScheduleRecord dailyScheduleRecord = mPersistenceManager.createDailyScheduleRecord(scheduleRecord.getId(), time);

                    schedules.add(new DailySchedule(domainFactory, new LocalDailyScheduleBridge(scheduleRecord, dailyScheduleRecord)));
                    break;
                }
                case WEEKLY: {
                    CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;

                    DayOfWeek dayOfWeek = weeklyScheduleData.DayOfWeek;
                    Time time = getTime(weeklyScheduleData.TimePair);

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.WEEKLY, startExactTimeStamp);

                    WeeklyScheduleRecord weeklyScheduleRecord = mPersistenceManager.createWeeklyScheduleRecord(scheduleRecord.getId(), dayOfWeek, time);

                    schedules.add(new WeeklySchedule(domainFactory, new LocalWeeklyScheduleBridge(scheduleRecord, weeklyScheduleRecord)));
                    break;
                }
                case MONTHLY_DAY: {
                    CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_DAY, startExactTimeStamp);

                    MonthlyDayScheduleRecord monthlyDayScheduleRecord = mPersistenceManager.createMonthlyDayScheduleRecord(scheduleRecord.getId(), monthlyDayScheduleData.mDayOfMonth, monthlyDayScheduleData.mBeginningOfMonth, getTime(monthlyDayScheduleData.TimePair));

                    schedules.add(new MonthlyDaySchedule(domainFactory, new LocalMonthlyDayScheduleBridge(scheduleRecord, monthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;

                    ScheduleRecord scheduleRecord = mPersistenceManager.createScheduleRecord(rootLocalTask, ScheduleType.MONTHLY_WEEK, startExactTimeStamp);

                    MonthlyWeekScheduleRecord monthlyWeekScheduleRecord = mPersistenceManager.createMonthlyWeekScheduleRecord(scheduleRecord.getId(), monthlyWeekScheduleData.mDayOfMonth, monthlyWeekScheduleData.mDayOfWeek, monthlyWeekScheduleData.mBeginningOfMonth, getTime(monthlyWeekScheduleData.TimePair));

                    schedules.add(new MonthlyWeekSchedule(domainFactory, new LocalMonthlyWeekScheduleBridge(scheduleRecord, monthlyWeekScheduleRecord)));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        return schedules;
    }

    @NonNull
    public Time getTime(@NonNull TimePair timePair) {
        if (timePair.mCustomTimeId != null) {
            Assert.assertTrue(timePair.mHourMinute == null);

            CustomTime customTime = mLocalCustomTimes.get(timePair.mCustomTimeId);
            Assert.assertTrue(customTime != null);

            return customTime;
        } else {
            Assert.assertTrue(timePair.mHourMinute != null);
            return new NormalTime(timePair.mHourMinute);
        }
    }
}
