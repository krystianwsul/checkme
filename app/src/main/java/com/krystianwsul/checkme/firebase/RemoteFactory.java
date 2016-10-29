package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.domainmodel.DailySchedule;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.Instance;
import com.krystianwsul.checkme.domainmodel.MonthlyDaySchedule;
import com.krystianwsul.checkme.domainmodel.MonthlyWeekSchedule;
import com.krystianwsul.checkme.domainmodel.Schedule;
import com.krystianwsul.checkme.domainmodel.SingleSchedule;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.domainmodel.WeeklySchedule;
import com.krystianwsul.checkme.domainmodel.local.LocalInstance;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy;
import com.krystianwsul.checkme.firebase.json.DailyScheduleJson;
import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.MonthlyDayScheduleJson;
import com.krystianwsul.checkme.firebase.json.MonthlyWeekScheduleJson;
import com.krystianwsul.checkme.firebase.json.SingleScheduleJson;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.firebase.json.WeeklyScheduleJson;
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord;
import com.krystianwsul.checkme.firebase.records.RemoteManager;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyWeekScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteSingleScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoteFactory {
    @NonNull
    private final RemoteManager mRemoteManager;

    @NonNull
    private final Map<String, RemoteTask> mRemoteTasks;

    @NonNull
    private final Map<String, RemoteTaskHierarchy> mRemoteTaskHierarchies;

    @NonNull
    private final Multimap<String, Schedule> mRemoteSchedules;

    @NonNull
    private final Map<String, RemoteInstance> mExistingRemoteInstances;

    public RemoteFactory(@NonNull DomainFactory domainFactory, @NonNull Iterable<DataSnapshot> children) {
        mRemoteManager = new RemoteManager(children);

        mRemoteTasks = Stream.of(mRemoteManager.mRemoteTaskRecords.values())
                .map(remoteTaskRecord -> new RemoteTask(domainFactory, remoteTaskRecord))
                .collect(Collectors.toMap(RemoteTask::getId, remoteTask -> remoteTask));

        mRemoteTaskHierarchies = Stream.of(mRemoteManager.mRemoteTaskHierarchyRecords.values())
                .map(remoteTaskHierarchyRecord -> new RemoteTaskHierarchy(domainFactory, remoteTaskHierarchyRecord))
                .collect(Collectors.toMap(RemoteTaskHierarchy::getId, remoteTaskHierarchy -> remoteTaskHierarchy));

        mRemoteSchedules = ArrayListMultimap.create();
        for (RemoteSingleScheduleRecord remoteSingleScheduleRecord : mRemoteManager.mRemoteSingleScheduleRecords.values())
            mRemoteSchedules.put(remoteSingleScheduleRecord.getTaskId(), new SingleSchedule(domainFactory, new RemoteSingleScheduleBridge(remoteSingleScheduleRecord)));

        for (RemoteDailyScheduleRecord remoteDailyScheduleRecord : mRemoteManager.mRemoteDailyScheduleRecords.values())
            mRemoteSchedules.put(remoteDailyScheduleRecord.getTaskId(), new DailySchedule(domainFactory, new RemoteDailyScheduleBridge(remoteDailyScheduleRecord)));

        for (RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord : mRemoteManager.mRemoteWeeklyScheduleRecords.values())
            mRemoteSchedules.put(remoteWeeklyScheduleRecord.getTaskId(), new WeeklySchedule(domainFactory, new RemoteWeeklyScheduleBridge(remoteWeeklyScheduleRecord)));

        for (RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord : mRemoteManager.mRemoteMonthlyDayScheduleRecords.values())
            mRemoteSchedules.put(remoteMonthlyDayScheduleRecord.getTaskId(), new MonthlyDaySchedule(domainFactory, new RemoteMonthlyDayScheduleBridge(remoteMonthlyDayScheduleRecord)));

        for (RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord : mRemoteManager.mRemoteMonthlyWeekScheduleRecords.values())
            mRemoteSchedules.put(remoteMonthlyWeekScheduleRecord.getTaskId(), new MonthlyWeekSchedule(domainFactory, new RemoteMonthlyWeekScheduleBridge(remoteMonthlyWeekScheduleRecord)));

        mExistingRemoteInstances = new HashMap<>();
        for (RemoteInstanceRecord remoteInstanceRecord : mRemoteManager.mRemoteInstanceRecords.values()) {
            InstanceShownRecord instanceShownRecord = domainFactory.getLocalFactory().getInstanceShownRecord(remoteInstanceRecord.getTaskId(), remoteInstanceRecord.getScheduleYear(), remoteInstanceRecord.getScheduleMonth(), remoteInstanceRecord.getScheduleDay(), remoteInstanceRecord.getScheduleCustomTimeId(), remoteInstanceRecord.getScheduleHour(), remoteInstanceRecord.getScheduleMinute());

            mExistingRemoteInstances.put(remoteInstanceRecord.getId(), new RemoteInstance(domainFactory, remoteInstanceRecord, instanceShownRecord));
        }
    }

    @NonNull
    public RemoteTask createScheduleRootTask(@NonNull DomainFactory domainFactory, @NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull Collection<String> friends) {
        RemoteTask remoteTask = createRemoteTaskHelper(domainFactory, now, name, note, friends);

        createSchedules(domainFactory, remoteTask.getRecordOf(), remoteTask.getId(), now, scheduleDatas);

        return remoteTask;
    }

    @NonNull
    public RemoteTask createRemoteTaskHelper(@NonNull DomainFactory domainFactory, @NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note, @NonNull Collection<String> friends) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note);

        UserData userData = MainActivity.getUserData();
        Assert.assertTrue(userData != null);

        Set<String> recordOf = new HashSet<>(friends);
        recordOf.add(UserData.getKey(userData.email));

        RemoteTaskRecord remoteTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(recordOf, taskJson));

        RemoteTask remoteTask = new RemoteTask(domainFactory, remoteTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));
        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        return remoteTask;
    }

    void createSchedules(@NonNull DomainFactory domainFactory, @NonNull Set<String> recordOf, @NonNull String taskId, @NonNull ExactTimeStamp now, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas) {
        Assert.assertTrue(!TextUtils.isEmpty(taskId));

        for (CreateTaskLoader.ScheduleData scheduleData : scheduleDatas) {
            Assert.assertTrue(scheduleData != null);

            switch (scheduleData.getScheduleType()) {
                case SINGLE: {
                    CreateTaskLoader.SingleScheduleData singleScheduleData = (CreateTaskLoader.SingleScheduleData) scheduleData;

                    Date date = singleScheduleData.Date;

                    Assert.assertTrue(singleScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = singleScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteSingleScheduleRecord remoteSingleScheduleRecord = mRemoteManager.newRemoteSingleScheduleRecord(new JsonWrapper(recordOf, new SingleScheduleJson(taskId, now.getLong(), null, date.getYear(), date.getMonth(), date.getDay(), null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(taskId, new SingleSchedule(domainFactory, new RemoteSingleScheduleBridge(remoteSingleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                    Assert.assertTrue(dailyScheduleData.TimePair.mCustomTimeId == null); // todo custom time

                    HourMinute hourMinute = dailyScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteDailyScheduleRecord remoteDailyScheduleRecord = mRemoteManager.newRemoteDailyScheduleRecord(new JsonWrapper(recordOf, new DailyScheduleJson(taskId, now.getLong(), null, null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(taskId, new DailySchedule(domainFactory, new RemoteDailyScheduleBridge(remoteDailyScheduleRecord)));
                    break;
                }
                case WEEKLY: {
                    CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;

                    DayOfWeek dayOfWeek = weeklyScheduleData.DayOfWeek;

                    Assert.assertTrue(weeklyScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = weeklyScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = mRemoteManager.newRemoteWeeklyScheduleRecord(new JsonWrapper(recordOf, new WeeklyScheduleJson(taskId, now.getLong(), null, dayOfWeek.ordinal(), null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(taskId, new WeeklySchedule(domainFactory, new RemoteWeeklyScheduleBridge(remoteWeeklyScheduleRecord)));
                    break;
                }
                case MONTHLY_DAY: {
                    CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;

                    Assert.assertTrue(monthlyDayScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = monthlyDayScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = mRemoteManager.newRemoteMonthlyDayScheduleRecord(new JsonWrapper(recordOf, new MonthlyDayScheduleJson(taskId, now.getLong(), null, monthlyDayScheduleData.mDayOfMonth, monthlyDayScheduleData.mBeginningOfMonth, null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(taskId, new MonthlyDaySchedule(domainFactory, new RemoteMonthlyDayScheduleBridge(remoteMonthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;

                    Assert.assertTrue(monthlyWeekScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = monthlyWeekScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = mRemoteManager.newRemoteMonthlyWeekScheduleRecord(new JsonWrapper(recordOf, new MonthlyWeekScheduleJson(taskId, now.getLong(), null, monthlyWeekScheduleData.mDayOfMonth, monthlyWeekScheduleData.mDayOfWeek.ordinal(), monthlyWeekScheduleData.mBeginningOfMonth, null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(taskId, new MonthlyWeekSchedule(domainFactory, new RemoteMonthlyWeekScheduleBridge(remoteMonthlyWeekScheduleRecord)));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    public void save() {
        Assert.assertTrue(!mRemoteManager.isSaved());

        mRemoteManager.save();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSaved() {
        return mRemoteManager.isSaved();
    }

    @NonNull
    RemoteTask createChildTask(@NonNull DomainFactory domainFactory, @NonNull RemoteTask parentTask, @NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note);
        RemoteTaskRecord childTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(parentTask.getRecordOf(), taskJson));

        RemoteTask childTask = new RemoteTask(domainFactory, childTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(childTask.getId()));

        mRemoteTasks.put(childTask.getId(), childTask);

        createTaskHierarchy(domainFactory, parentTask, childTask, now);

        return childTask;
    }

    void createTaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull RemoteTask parentRemoteTask, @NonNull RemoteTask childRemoteTask, @NonNull ExactTimeStamp now) {
        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(parentRemoteTask.getId(), childRemoteTask.getId(), now.getLong(), null);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteManager.newRemoteTaskHierarchyRecord(new JsonWrapper(parentRemoteTask.getRecordOf(), taskHierarchyJson));

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(domainFactory, remoteTaskHierarchyRecord);
        Assert.assertTrue(!mRemoteTaskHierarchies.containsKey(remoteTaskHierarchy.getId()));

        mRemoteTaskHierarchies.put(remoteTaskHierarchy.getId(), remoteTaskHierarchy);
    }

    @NonNull
    RemoteInstanceRecord createRemoteInstanceRecord(@NonNull RemoteTask remoteTask, @NonNull RemoteInstance remoteInstance, @NonNull DateTime scheduleDateTime, @NonNull ExactTimeStamp now) {
        HourMinute hourMinute = scheduleDateTime.getTime().getTimePair().mHourMinute;
        Integer hour = (hourMinute == null ? null : hourMinute.getHour());
        Integer minute = (hourMinute == null ? null : hourMinute.getMinute());
        InstanceJson instanceJson = new InstanceJson(remoteTask.getId(), null, scheduleDateTime.getDate().getYear(), scheduleDateTime.getDate().getMonth(), scheduleDateTime.getDate().getDay(), scheduleDateTime.getTime().getTimePair().mCustomTimeId, hour, minute, null, null, null, null, null, null, now.getLong());

        JsonWrapper jsonWrapper = new JsonWrapper(remoteTask.getRecordOf(), instanceJson);

        RemoteInstanceRecord remoteInstanceRecord = mRemoteManager.newRemoteInstanceRecord(jsonWrapper);
        Assert.assertTrue(!mExistingRemoteInstances.containsKey(remoteInstanceRecord.getId()));

        mExistingRemoteInstances.put(remoteInstanceRecord.getId(), remoteInstance);

        return remoteInstanceRecord;
    }

    public void removeIrrelevant(@NonNull DomainFactory.Irrelevant irrelevant) {
        List<RemoteTaskHierarchy> irrelevantTaskHierarchies = Stream.of(mRemoteTaskHierarchies.values())
                .filter(taskHierarchy -> irrelevant.mTasks.contains(taskHierarchy.getChildTask()))
                .collect(Collectors.toList());

        Assert.assertTrue(Stream.of(irrelevantTaskHierarchies)
                .allMatch(taskHierarchy -> irrelevant.mTasks.contains(taskHierarchy.getParentTask())));

        for (RemoteTaskHierarchy irrelevantTaskHierarchy : irrelevantTaskHierarchies)
            mRemoteTaskHierarchies.remove(irrelevantTaskHierarchy.getId());

        for (Task task : irrelevant.mTasks) {
            if (task instanceof RemoteTask) {
                Assert.assertTrue(mRemoteTasks.containsKey(((RemoteTask) task).getId()));

                mRemoteTasks.remove(((RemoteTask) task).getId());
            }
        }

        for (Instance instance : irrelevant.mInstances) {
            if (instance instanceof RemoteInstance) {
                Assert.assertTrue(mExistingRemoteInstances.containsKey(((RemoteInstance) instance).getId()));

                mExistingRemoteInstances.remove(((RemoteInstance) instance).getId());
            }
        }

        // todo customTimes
    }

    @NonNull
    public RemoteTask copyLocalTask(@NonNull DomainFactory domainFactory, @NonNull LocalTask localTask, @NonNull Set<String> recordOf) {
        Long endTime = (localTask.getEndExactTimeStamp() != null ? localTask.getEndExactTimeStamp().getLong() : null);
        Assert.assertTrue(!recordOf.isEmpty());

        Date oldestVisible = localTask.getOldestVisible();
        Integer oldestVisibleYear;
        Integer oldestVisibleMonth;
        Integer oldestVisibleDay;
        if (oldestVisible != null) {
            oldestVisibleYear = oldestVisible.getYear();
            oldestVisibleMonth = oldestVisible.getMonth();
            oldestVisibleDay = oldestVisible.getDay();
        } else {
            oldestVisibleYear = null;
            oldestVisibleMonth = null;
            oldestVisibleDay = null;
        }

        TaskJson taskJson = new TaskJson(localTask.getName(), localTask.getStartExactTimeStamp().getLong(), endTime, oldestVisibleYear, oldestVisibleMonth, oldestVisibleDay, localTask.getNote());
        RemoteTaskRecord remoteTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(recordOf, taskJson));

        RemoteTask remoteTask = new RemoteTask(domainFactory, remoteTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));

        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        for (Schedule schedule : localTask.getSchedules()) {
            Assert.assertTrue(schedule != null);

            switch (schedule.getType()) {
                case SINGLE: {
                    SingleSchedule singleSchedule = (SingleSchedule) schedule;

                    Date date = singleSchedule.getDate();

                    Assert.assertTrue(singleSchedule.getCustomTimeId() == null); // todo custom time

                    HourMinute hourMinute = singleSchedule.getHourMinute();
                    Assert.assertTrue(hourMinute != null);

                    RemoteSingleScheduleRecord remoteSingleScheduleRecord = mRemoteManager.newRemoteSingleScheduleRecord(new JsonWrapper(recordOf, new SingleScheduleJson(remoteTask.getId(), singleSchedule.getStartTime(), singleSchedule.getEndTime(), date.getYear(), date.getMonth(), date.getDay(), null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(remoteTask.getId(), new SingleSchedule(domainFactory, new RemoteSingleScheduleBridge(remoteSingleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    DailySchedule dailySchedule = (DailySchedule) schedule;

                    Assert.assertTrue(dailySchedule.getCustomTimeId() == null); // todo custom time

                    HourMinute hourMinute = dailySchedule.getHourMinute();
                    Assert.assertTrue(hourMinute != null);

                    RemoteDailyScheduleRecord remoteDailyScheduleRecord = mRemoteManager.newRemoteDailyScheduleRecord(new JsonWrapper(recordOf, new DailyScheduleJson(remoteTask.getId(), dailySchedule.getStartTime(), dailySchedule.getEndTime(), null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(remoteTask.getId(), new DailySchedule(domainFactory, new RemoteDailyScheduleBridge(remoteDailyScheduleRecord)));
                    break;
                }
                case WEEKLY: {
                    WeeklySchedule weeklySchedule = (WeeklySchedule) schedule;

                    DayOfWeek dayOfWeek = weeklySchedule.getDayOfWeek();

                    Assert.assertTrue(weeklySchedule.getCustomTimeId() == null); // todo custom time

                    HourMinute hourMinute = weeklySchedule.getHourMinute();
                    Assert.assertTrue(hourMinute != null);

                    RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = mRemoteManager.newRemoteWeeklyScheduleRecord(new JsonWrapper(recordOf, new WeeklyScheduleJson(remoteTask.getId(), weeklySchedule.getStartTime(), weeklySchedule.getEndTime(), dayOfWeek.ordinal(), null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(remoteTask.getId(), new WeeklySchedule(domainFactory, new RemoteWeeklyScheduleBridge(remoteWeeklyScheduleRecord)));
                    break;
                }
                case MONTHLY_DAY: {
                    MonthlyDaySchedule monthlyDaySchedule = (MonthlyDaySchedule) schedule;

                    Assert.assertTrue(monthlyDaySchedule.getCustomTimeId() == null); // todo custom time

                    HourMinute hourMinute = monthlyDaySchedule.getHourMinute();
                    Assert.assertTrue(hourMinute != null);

                    RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = mRemoteManager.newRemoteMonthlyDayScheduleRecord(new JsonWrapper(recordOf, new MonthlyDayScheduleJson(remoteTask.getId(), monthlyDaySchedule.getStartTime(), monthlyDaySchedule.getEndTime(), monthlyDaySchedule.getDayOfMonth(), monthlyDaySchedule.getBeginningOfMonth(), null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(remoteTask.getId(), new MonthlyDaySchedule(domainFactory, new RemoteMonthlyDayScheduleBridge(remoteMonthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    MonthlyWeekSchedule monthlyWeekSchedule = (MonthlyWeekSchedule) schedule;

                    Assert.assertTrue(monthlyWeekSchedule.getCustomTimeId() == null); // todo custom time

                    HourMinute hourMinute = monthlyWeekSchedule.getHourMinute();
                    Assert.assertTrue(hourMinute != null);

                    RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = mRemoteManager.newRemoteMonthlyWeekScheduleRecord(new JsonWrapper(recordOf, new MonthlyWeekScheduleJson(remoteTask.getId(), monthlyWeekSchedule.getStartTime(), monthlyWeekSchedule.getEndTime(), monthlyWeekSchedule.getDayOfMonth(), monthlyWeekSchedule.getDayOfWeek().ordinal(), monthlyWeekSchedule.getBeginningOfMonth(), null, hourMinute.getHour(), hourMinute.getMinute())));

                    mRemoteSchedules.put(remoteTask.getId(), new MonthlyWeekSchedule(domainFactory, new RemoteMonthlyWeekScheduleBridge(remoteMonthlyWeekScheduleRecord)));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        return remoteTask;
    }

    @NonNull
    public RemoteTaskHierarchy copyLocalTaskHierarchy(@NonNull DomainFactory domainFactory, @NonNull LocalTaskHierarchy localTaskHierarchy, @NonNull Set<String> recordOf, @NonNull String remoteParentTaskId, @NonNull String remoteChildTaskId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteParentTaskId));
        Assert.assertTrue(!TextUtils.isEmpty(remoteChildTaskId));
        Assert.assertTrue(!recordOf.isEmpty());

        Long endTime = (localTaskHierarchy.getEndExactTimeStamp() != null ? localTaskHierarchy.getEndExactTimeStamp().getLong() : null);

        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, localTaskHierarchy.getStartExactTimeStamp().getLong(), endTime);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteManager.newRemoteTaskHierarchyRecord(new JsonWrapper(recordOf, taskHierarchyJson));

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(domainFactory, remoteTaskHierarchyRecord);
        Assert.assertTrue(!mRemoteTaskHierarchies.containsKey(remoteTaskHierarchy.getId()));

        mRemoteTaskHierarchies.put(remoteTaskHierarchy.getId(), remoteTaskHierarchy);

        return remoteTaskHierarchy;
    }

    @NonNull
    public RemoteInstance copyLocalInstance(@NonNull DomainFactory domainFactory, @NonNull LocalInstance localInstance, @NonNull Set<String> recordOf, @NonNull String remoteTaskId) {
        Assert.assertTrue(!recordOf.isEmpty());
        Assert.assertTrue(!TextUtils.isEmpty(remoteTaskId));

        Long done = (localInstance.getDone() != null ? localInstance.getDone().getLong() : null);

        Date scheduleDate = localInstance.getScheduleDate();
        TimePair scheduleTimePair = localInstance.getScheduleTimePair();

        Integer scheduleHour;
        Integer scheduleMinute;
        if (scheduleTimePair.mHourMinute != null) {
            scheduleHour = scheduleTimePair.mHourMinute.getHour();
            scheduleMinute = scheduleTimePair.mHourMinute.getMinute();
        } else {
            scheduleHour = null;
            scheduleMinute = null;
        }

        Date instanceDate = localInstance.getInstanceDate();
        TimePair instanceTimePair = localInstance.getInstanceTimePair();

        Integer instanceHour;
        Integer instanceMinute;
        if (instanceTimePair.mHourMinute != null) {
            instanceHour = instanceTimePair.mHourMinute.getHour();
            instanceMinute = instanceTimePair.mHourMinute.getMinute();
        } else {
            instanceHour = null;
            instanceMinute = null;
        }

        InstanceJson instanceJson = new InstanceJson(remoteTaskId, done, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), scheduleTimePair.mCustomTimeId, scheduleHour, scheduleMinute, instanceDate.getYear(), instanceDate.getMonth(), instanceDate.getDay(), instanceTimePair.mCustomTimeId, instanceHour, instanceMinute, localInstance.getHierarchyTime());
        RemoteInstanceRecord remoteInstanceRecord = new RemoteInstanceRecord(new JsonWrapper(recordOf, instanceJson));

        InstanceShownRecord instanceShownRecord;
        if (localInstance.getNotificationShown() || localInstance.getNotified()) {
            instanceShownRecord = domainFactory.getLocalFactory().createInstanceShownRecord(remoteTaskId, localInstance.getScheduleDateTime());
        } else {
            instanceShownRecord = null;
        }

        RemoteInstance remoteInstance = new RemoteInstance(domainFactory, remoteInstanceRecord, instanceShownRecord);
        Assert.assertTrue(!mExistingRemoteInstances.containsKey(remoteInstance.getId()));

        mExistingRemoteInstances.put(remoteInstance.getId(), remoteInstance);

        return remoteInstance;
    }

    public void updateRecordOf(@NonNull RemoteTask startingRemoteTask, @NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        UpdateRecordOfData updateRecordOfData = new UpdateRecordOfData();

        updateRecordOfHelper(updateRecordOfData, startingRemoteTask);

        for (RemoteTask remoteTask : updateRecordOfData.mRemoteTasks)
            remoteTask.updateRecordOf(addedFriends, removedFriends);

        for (RemoteTaskHierarchy remoteTaskHierarchy : updateRecordOfData.mRemoteTaskHierarchies)
            remoteTaskHierarchy.updateRecordOf(addedFriends, removedFriends);

        for (RemoteInstance remoteInstance : updateRecordOfData.mRemoteInstances)
            remoteInstance.updateRecordOf(addedFriends, removedFriends);
    }

    private void updateRecordOfHelper(@NonNull UpdateRecordOfData updateRecordOfData, @NonNull RemoteTask remoteTask) {
        if (updateRecordOfData.mRemoteTasks.contains(remoteTask))
            return;

        TaskKey taskKey = remoteTask.getTaskKey();

        updateRecordOfData.mRemoteTasks.add(remoteTask);

        List<RemoteTaskHierarchy> parentRemoteTaskHierarchies = Stream.of(mRemoteTaskHierarchies.values())
                .filter(remoteTaskHierarchy -> remoteTaskHierarchy.getChildTaskKey().equals(taskKey))
                .collect(Collectors.toList());

        updateRecordOfData.mRemoteTaskHierarchies.addAll(parentRemoteTaskHierarchies);

        updateRecordOfData.mRemoteInstances.addAll(Stream.of(mExistingRemoteInstances.values())
                .filter(remoteInstance -> remoteInstance.getTaskKey().equals(taskKey))
                .collect(Collectors.toList()));

        Stream.of(mRemoteTaskHierarchies.values())
                .filter(localTaskHierarchy -> localTaskHierarchy.getParentTaskKey().equals(taskKey))
                .map(RemoteTaskHierarchy::getChildTask)
                .forEach(childTask -> updateRecordOfHelper(updateRecordOfData, (RemoteTask) childTask));

        Stream.of(parentRemoteTaskHierarchies)
                .map(RemoteTaskHierarchy::getParentTask)
                .forEach(parentTask -> updateRecordOfHelper(updateRecordOfData, (RemoteTask) parentTask));
    }

    private static class UpdateRecordOfData {
        final List<RemoteTask> mRemoteTasks = new ArrayList<>();
        final List<RemoteTaskHierarchy> mRemoteTaskHierarchies = new ArrayList<>();
        final List<RemoteInstance> mRemoteInstances = new ArrayList<>();
    }

    @NonNull
    public Map<String, RemoteTask> getTasks() {
        return mRemoteTasks;
    }

    @NonNull
    public Collection<RemoteTaskHierarchy> getTaskHierarchies() {
        return mRemoteTaskHierarchies.values();
    }

    @NonNull
    public Collection<RemoteInstance> getExistingInstances() {
        return mExistingRemoteInstances.values();
    }

    @NonNull
    Collection<Schedule> getSchedules(@NonNull String taskId) {
        if (mRemoteSchedules.containsKey(taskId))
            return mRemoteSchedules.get(taskId);
        else
            return new ArrayList<>();
    }
}
