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
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.domainmodel.local.LocalInstance;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy;
import com.krystianwsul.checkme.firebase.json.CustomTimeJson;
import com.krystianwsul.checkme.firebase.json.DailyScheduleJson;
import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.MonthlyDayScheduleJson;
import com.krystianwsul.checkme.firebase.json.MonthlyWeekScheduleJson;
import com.krystianwsul.checkme.firebase.json.SingleScheduleJson;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.firebase.json.WeeklyScheduleJson;
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord;
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
import com.krystianwsul.checkme.utils.CustomTimeKey;
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
    private final DomainFactory mDomainFactory;

    @NonNull
    private final UserData mUserData;

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

    @NonNull
    public final Map<String, RemoteCustomTime> mRemoteCustomTimes; // todo customtimes scope

    public RemoteFactory(@NonNull DomainFactory domainFactory, @NonNull Iterable<DataSnapshot> children, @NonNull UserData userData) {
        mDomainFactory = domainFactory;
        mUserData = userData;

        mRemoteManager = new RemoteManager(children);

        mRemoteTasks = Stream.of(mRemoteManager.mRemoteTaskRecords.values())
                .map(remoteTaskRecord -> new RemoteTask(domainFactory, remoteTaskRecord))
                .collect(Collectors.toMap(RemoteTask::getId, remoteTask -> remoteTask));

        mRemoteTaskHierarchies = Stream.of(mRemoteManager.mRemoteTaskHierarchyRecords.values())
                .map(remoteTaskHierarchyRecord -> new RemoteTaskHierarchy(domainFactory, remoteTaskHierarchyRecord))
                .collect(Collectors.toMap(RemoteTaskHierarchy::getId, remoteTaskHierarchy -> remoteTaskHierarchy));

        mRemoteSchedules = ArrayListMultimap.create();
        for (RemoteSingleScheduleRecord remoteSingleScheduleRecord : mRemoteManager.mRemoteSingleScheduleRecords.values())
            mRemoteSchedules.put(remoteSingleScheduleRecord.getTaskId(), new SingleSchedule(domainFactory, new RemoteSingleScheduleBridge(domainFactory, remoteSingleScheduleRecord)));

        for (RemoteDailyScheduleRecord remoteDailyScheduleRecord : mRemoteManager.mRemoteDailyScheduleRecords.values())
            mRemoteSchedules.put(remoteDailyScheduleRecord.getTaskId(), new DailySchedule(domainFactory, new RemoteDailyScheduleBridge(domainFactory, remoteDailyScheduleRecord)));

        for (RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord : mRemoteManager.mRemoteWeeklyScheduleRecords.values())
            mRemoteSchedules.put(remoteWeeklyScheduleRecord.getTaskId(), new WeeklySchedule(domainFactory, new RemoteWeeklyScheduleBridge(domainFactory, remoteWeeklyScheduleRecord)));

        for (RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord : mRemoteManager.mRemoteMonthlyDayScheduleRecords.values())
            mRemoteSchedules.put(remoteMonthlyDayScheduleRecord.getTaskId(), new MonthlyDaySchedule(domainFactory, new RemoteMonthlyDayScheduleBridge(domainFactory, remoteMonthlyDayScheduleRecord)));

        for (RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord : mRemoteManager.mRemoteMonthlyWeekScheduleRecords.values())
            mRemoteSchedules.put(remoteMonthlyWeekScheduleRecord.getTaskId(), new MonthlyWeekSchedule(domainFactory, new RemoteMonthlyWeekScheduleBridge(domainFactory, remoteMonthlyWeekScheduleRecord)));

        mExistingRemoteInstances = new HashMap<>();
        for (RemoteInstanceRecord remoteInstanceRecord : mRemoteManager.mRemoteInstanceRecords.values()) {
            InstanceShownRecord instanceShownRecord = domainFactory.getLocalFactory().getInstanceShownRecord(remoteInstanceRecord.getTaskId(), remoteInstanceRecord.getScheduleYear(), remoteInstanceRecord.getScheduleMonth(), remoteInstanceRecord.getScheduleDay(), remoteInstanceRecord.getScheduleCustomTimeId(), remoteInstanceRecord.getScheduleHour(), remoteInstanceRecord.getScheduleMinute());

            mExistingRemoteInstances.put(remoteInstanceRecord.getId(), new RemoteInstance(domainFactory, remoteInstanceRecord, instanceShownRecord));
        }

        String userId = UserData.getKey(userData.email);

        mRemoteCustomTimes = new HashMap<>();

        for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteManager.mRemoteCustomTimeRecords.values()) {
            Assert.assertTrue(remoteCustomTimeRecord != null);

            if (remoteCustomTimeRecord.getOwnerId().equals(userId)) {
                if (domainFactory.getLocalFactory().mLocalCustomTimes.containsKey(remoteCustomTimeRecord.getLocalId())) {
                    LocalCustomTime localCustomTime = domainFactory.getLocalFactory().mLocalCustomTimes.get(remoteCustomTimeRecord.getLocalId());
                    Assert.assertTrue(localCustomTime != null);

                    localCustomTime.setRemoteCustomTimeRecord(remoteCustomTimeRecord);
                } else {
                    // Albo jakiś syf, albo localCustomTime został usunięty gdy nie było połączenia.

                    remoteCustomTimeRecord.delete(); // faktyczne usunięcie nastąpi przy następnym zapisywaniu czegoś innego
                }
            } else {
                RemoteCustomTime remoteCustomTime = new RemoteCustomTime(domainFactory, remoteCustomTimeRecord);

                Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTime.getCustomTimeKey().mRemoteCustomTimeId));
                Assert.assertTrue(!mRemoteCustomTimes.containsKey(remoteCustomTime.getCustomTimeKey().mRemoteCustomTimeId));

                mRemoteCustomTimes.put(remoteCustomTime.getCustomTimeKey().mRemoteCustomTimeId, remoteCustomTime);
            }
        }
    }

    @NonNull
    public RemoteTask createScheduleRootTask(@NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull Collection<String> friends) {
        RemoteTask remoteTask = createRemoteTaskHelper(now, name, note, friends);

        createSchedules(remoteTask.getRecordOf(), remoteTask.getId(), now, scheduleDatas);

        return remoteTask;
    }

    @NonNull
    public RemoteTask createRemoteTaskHelper(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note, @NonNull Collection<String> friends) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note);

        UserData userData = MainActivity.getUserData();
        Assert.assertTrue(userData != null);

        Set<String> recordOf = new HashSet<>(friends);
        recordOf.add(UserData.getKey(userData.email));

        RemoteTaskRecord remoteTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(recordOf, taskJson));

        RemoteTask remoteTask = new RemoteTask(mDomainFactory, remoteTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));
        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        return remoteTask;
    }

    void createSchedules(@NonNull Set<String> recordOf, @NonNull String taskId, @NonNull ExactTimeStamp now, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas) {
        Assert.assertTrue(!TextUtils.isEmpty(taskId));

        for (CreateTaskLoader.ScheduleData scheduleData : scheduleDatas) {
            Assert.assertTrue(scheduleData != null);

            switch (scheduleData.getScheduleType()) {
                case SINGLE: {
                    CreateTaskLoader.SingleScheduleData singleScheduleData = (CreateTaskLoader.SingleScheduleData) scheduleData;

                    Date date = singleScheduleData.Date;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (singleScheduleData.TimePair.mCustomTimeKey != null) {
                        Assert.assertTrue(singleScheduleData.TimePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(singleScheduleData.TimePair.mCustomTimeKey, recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(singleScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = singleScheduleData.TimePair.mHourMinute.getHour();
                        minute = singleScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteSingleScheduleRecord remoteSingleScheduleRecord = mRemoteManager.newRemoteSingleScheduleRecord(new JsonWrapper(recordOf, new SingleScheduleJson(taskId, now.getLong(), null, date.getYear(), date.getMonth(), date.getDay(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(taskId, new SingleSchedule(mDomainFactory, new RemoteSingleScheduleBridge(mDomainFactory, remoteSingleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (dailyScheduleData.TimePair.mCustomTimeKey != null) {
                        Assert.assertTrue(dailyScheduleData.TimePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(dailyScheduleData.TimePair.mCustomTimeKey, recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(dailyScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = dailyScheduleData.TimePair.mHourMinute.getHour();
                        minute = dailyScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteDailyScheduleRecord remoteDailyScheduleRecord = mRemoteManager.newRemoteDailyScheduleRecord(new JsonWrapper(recordOf, new DailyScheduleJson(taskId, now.getLong(), null, remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(taskId, new DailySchedule(mDomainFactory, new RemoteDailyScheduleBridge(mDomainFactory, remoteDailyScheduleRecord)));
                    break;
                }
                case WEEKLY: {
                    CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;

                    DayOfWeek dayOfWeek = weeklyScheduleData.DayOfWeek;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (weeklyScheduleData.TimePair.mCustomTimeKey != null) {
                        Assert.assertTrue(weeklyScheduleData.TimePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(weeklyScheduleData.TimePair.mCustomTimeKey, recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(weeklyScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = weeklyScheduleData.TimePair.mHourMinute.getHour();
                        minute = weeklyScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = mRemoteManager.newRemoteWeeklyScheduleRecord(new JsonWrapper(recordOf, new WeeklyScheduleJson(taskId, now.getLong(), null, dayOfWeek.ordinal(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(taskId, new WeeklySchedule(mDomainFactory, new RemoteWeeklyScheduleBridge(mDomainFactory, remoteWeeklyScheduleRecord)));
                    break;
                }
                case MONTHLY_DAY: {
                    CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (monthlyDayScheduleData.TimePair.mCustomTimeKey != null) {
                        Assert.assertTrue(monthlyDayScheduleData.TimePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(monthlyDayScheduleData.TimePair.mCustomTimeKey, recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(monthlyDayScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = monthlyDayScheduleData.TimePair.mHourMinute.getHour();
                        minute = monthlyDayScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = mRemoteManager.newRemoteMonthlyDayScheduleRecord(new JsonWrapper(recordOf, new MonthlyDayScheduleJson(taskId, now.getLong(), null, monthlyDayScheduleData.mDayOfMonth, monthlyDayScheduleData.mBeginningOfMonth, remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(taskId, new MonthlyDaySchedule(mDomainFactory, new RemoteMonthlyDayScheduleBridge(mDomainFactory, remoteMonthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (monthlyWeekScheduleData.TimePair.mCustomTimeKey != null) {
                        Assert.assertTrue(monthlyWeekScheduleData.TimePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(monthlyWeekScheduleData.TimePair.mCustomTimeKey, recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(monthlyWeekScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = monthlyWeekScheduleData.TimePair.mHourMinute.getHour();
                        minute = monthlyWeekScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = mRemoteManager.newRemoteMonthlyWeekScheduleRecord(new JsonWrapper(recordOf, new MonthlyWeekScheduleJson(taskId, now.getLong(), null, monthlyWeekScheduleData.mDayOfMonth, monthlyWeekScheduleData.mDayOfWeek.ordinal(), monthlyWeekScheduleData.mBeginningOfMonth, remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(taskId, new MonthlyWeekSchedule(mDomainFactory, new RemoteMonthlyWeekScheduleBridge(mDomainFactory, remoteMonthlyWeekScheduleRecord)));
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
    RemoteTask createChildTask(@NonNull RemoteTask parentTask, @NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note);
        RemoteTaskRecord childTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(parentTask.getRecordOf(), taskJson));

        RemoteTask childTask = new RemoteTask(mDomainFactory, childTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(childTask.getId()));

        mRemoteTasks.put(childTask.getId(), childTask);

        createTaskHierarchy(parentTask, childTask, now);

        return childTask;
    }

    void createTaskHierarchy(@NonNull RemoteTask parentRemoteTask, @NonNull RemoteTask childRemoteTask, @NonNull ExactTimeStamp now) {
        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(parentRemoteTask.getId(), childRemoteTask.getId(), now.getLong(), null);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteManager.newRemoteTaskHierarchyRecord(new JsonWrapper(parentRemoteTask.getRecordOf(), taskHierarchyJson));

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(mDomainFactory, remoteTaskHierarchyRecord);
        Assert.assertTrue(!mRemoteTaskHierarchies.containsKey(remoteTaskHierarchy.getId()));

        mRemoteTaskHierarchies.put(remoteTaskHierarchy.getId(), remoteTaskHierarchy);
    }

    @NonNull
    RemoteInstanceRecord createRemoteInstanceRecord(@NonNull RemoteTask remoteTask, @NonNull RemoteInstance remoteInstance, @NonNull DateTime scheduleDateTime, @NonNull ExactTimeStamp now) {
        String remoteCustomTimeId;
        Integer hour;
        Integer minute;

        CustomTimeKey customTimeKey = scheduleDateTime.getTime().getTimePair().mCustomTimeKey;
        HourMinute hourMinute = scheduleDateTime.getTime().getTimePair().mHourMinute;

        if (customTimeKey != null) {
            Assert.assertTrue(hourMinute == null);

            remoteCustomTimeId = mDomainFactory.getRemoteCustomTimeId(customTimeKey);

            hour = null;
            minute = null;
        } else {
            Assert.assertTrue(hourMinute != null);

            remoteCustomTimeId = null;

            hour = hourMinute.getHour();
            minute = hourMinute.getMinute();
        }

        InstanceJson instanceJson = new InstanceJson(remoteTask.getId(), null, scheduleDateTime.getDate().getYear(), scheduleDateTime.getDate().getMonth(), scheduleDateTime.getDate().getDay(), remoteCustomTimeId, hour, minute, null, null, null, null, null, null, now.getLong());

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
    }

    @NonNull
    public RemoteTask copyLocalTask(@NonNull LocalTask localTask, @NonNull Set<String> recordOf) {
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

        RemoteTask remoteTask = new RemoteTask(mDomainFactory, remoteTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));

        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        for (Schedule schedule : localTask.getSchedules()) {
            Assert.assertTrue(schedule != null);

            switch (schedule.getType()) {
                case SINGLE: {
                    SingleSchedule singleSchedule = (SingleSchedule) schedule;

                    Date date = singleSchedule.getDate();

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (singleSchedule.getCustomTimeKey() != null) {
                        Assert.assertTrue(singleSchedule.getHourMinute() == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(singleSchedule.getCustomTimeKey(), recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(singleSchedule.getHourMinute() != null);

                        remoteCustomTimeId = null;
                        hour = singleSchedule.getHourMinute().getHour();
                        minute = singleSchedule.getHourMinute().getMinute();
                    }

                    RemoteSingleScheduleRecord remoteSingleScheduleRecord = mRemoteManager.newRemoteSingleScheduleRecord(new JsonWrapper(recordOf, new SingleScheduleJson(remoteTask.getId(), singleSchedule.getStartTime(), singleSchedule.getEndTime(), date.getYear(), date.getMonth(), date.getDay(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(remoteTask.getId(), new SingleSchedule(mDomainFactory, new RemoteSingleScheduleBridge(mDomainFactory, remoteSingleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    DailySchedule dailySchedule = (DailySchedule) schedule;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (dailySchedule.getCustomTimeKey() != null) {
                        Assert.assertTrue(dailySchedule.getHourMinute() == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(dailySchedule.getCustomTimeKey(), recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(dailySchedule.getHourMinute() != null);

                        remoteCustomTimeId = null;
                        hour = dailySchedule.getHourMinute().getHour();
                        minute = dailySchedule.getHourMinute().getMinute();
                    }

                    RemoteDailyScheduleRecord remoteDailyScheduleRecord = mRemoteManager.newRemoteDailyScheduleRecord(new JsonWrapper(recordOf, new DailyScheduleJson(remoteTask.getId(), dailySchedule.getStartTime(), dailySchedule.getEndTime(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(remoteTask.getId(), new DailySchedule(mDomainFactory, new RemoteDailyScheduleBridge(mDomainFactory, remoteDailyScheduleRecord)));
                    break;
                }
                case WEEKLY: {
                    WeeklySchedule weeklySchedule = (WeeklySchedule) schedule;

                    DayOfWeek dayOfWeek = weeklySchedule.getDayOfWeek();

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (weeklySchedule.getCustomTimeKey() != null) {
                        Assert.assertTrue(weeklySchedule.getHourMinute() == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(weeklySchedule.getCustomTimeKey(), recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(weeklySchedule.getHourMinute() != null);

                        remoteCustomTimeId = null;
                        hour = weeklySchedule.getHourMinute().getHour();
                        minute = weeklySchedule.getHourMinute().getMinute();
                    }

                    RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = mRemoteManager.newRemoteWeeklyScheduleRecord(new JsonWrapper(recordOf, new WeeklyScheduleJson(remoteTask.getId(), weeklySchedule.getStartTime(), weeklySchedule.getEndTime(), dayOfWeek.ordinal(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(remoteTask.getId(), new WeeklySchedule(mDomainFactory, new RemoteWeeklyScheduleBridge(mDomainFactory, remoteWeeklyScheduleRecord)));
                    break;
                }
                case MONTHLY_DAY: {
                    MonthlyDaySchedule monthlyDaySchedule = (MonthlyDaySchedule) schedule;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (monthlyDaySchedule.getCustomTimeKey() != null) {
                        Assert.assertTrue(monthlyDaySchedule.getHourMinute() == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(monthlyDaySchedule.getCustomTimeKey(), recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(monthlyDaySchedule.getHourMinute() != null);

                        remoteCustomTimeId = null;
                        hour = monthlyDaySchedule.getHourMinute().getHour();
                        minute = monthlyDaySchedule.getHourMinute().getMinute();
                    }

                    RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = mRemoteManager.newRemoteMonthlyDayScheduleRecord(new JsonWrapper(recordOf, new MonthlyDayScheduleJson(remoteTask.getId(), monthlyDaySchedule.getStartTime(), monthlyDaySchedule.getEndTime(), monthlyDaySchedule.getDayOfMonth(), monthlyDaySchedule.getBeginningOfMonth(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(remoteTask.getId(), new MonthlyDaySchedule(mDomainFactory, new RemoteMonthlyDayScheduleBridge(mDomainFactory, remoteMonthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    MonthlyWeekSchedule monthlyWeekSchedule = (MonthlyWeekSchedule) schedule;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (monthlyWeekSchedule.getCustomTimeKey() != null) {
                        Assert.assertTrue(monthlyWeekSchedule.getHourMinute() == null);

                        remoteCustomTimeId = getRemoteCustomTimeId(monthlyWeekSchedule.getCustomTimeKey(), recordOf);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(monthlyWeekSchedule.getHourMinute() != null);

                        remoteCustomTimeId = null;
                        hour = monthlyWeekSchedule.getHourMinute().getHour();
                        minute = monthlyWeekSchedule.getHourMinute().getMinute();
                    }

                    RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = mRemoteManager.newRemoteMonthlyWeekScheduleRecord(new JsonWrapper(recordOf, new MonthlyWeekScheduleJson(remoteTask.getId(), monthlyWeekSchedule.getStartTime(), monthlyWeekSchedule.getEndTime(), monthlyWeekSchedule.getDayOfMonth(), monthlyWeekSchedule.getDayOfWeek().ordinal(), monthlyWeekSchedule.getBeginningOfMonth(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.put(remoteTask.getId(), new MonthlyWeekSchedule(mDomainFactory, new RemoteMonthlyWeekScheduleBridge(mDomainFactory, remoteMonthlyWeekScheduleRecord)));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        return remoteTask;
    }

    @NonNull
    public RemoteTaskHierarchy copyLocalTaskHierarchy(@NonNull LocalTaskHierarchy localTaskHierarchy, @NonNull Set<String> recordOf, @NonNull String remoteParentTaskId, @NonNull String remoteChildTaskId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteParentTaskId));
        Assert.assertTrue(!TextUtils.isEmpty(remoteChildTaskId));
        Assert.assertTrue(!recordOf.isEmpty());

        Long endTime = (localTaskHierarchy.getEndExactTimeStamp() != null ? localTaskHierarchy.getEndExactTimeStamp().getLong() : null);

        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, localTaskHierarchy.getStartExactTimeStamp().getLong(), endTime);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteManager.newRemoteTaskHierarchyRecord(new JsonWrapper(recordOf, taskHierarchyJson));

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(mDomainFactory, remoteTaskHierarchyRecord);
        Assert.assertTrue(!mRemoteTaskHierarchies.containsKey(remoteTaskHierarchy.getId()));

        mRemoteTaskHierarchies.put(remoteTaskHierarchy.getId(), remoteTaskHierarchy);

        return remoteTaskHierarchy;
    }

    @NonNull
    public RemoteInstance copyLocalInstance(@NonNull LocalInstance localInstance, @NonNull Set<String> recordOf, @NonNull String remoteTaskId) {
        Assert.assertTrue(!recordOf.isEmpty());
        Assert.assertTrue(!TextUtils.isEmpty(remoteTaskId));

        Long done = (localInstance.getDone() != null ? localInstance.getDone().getLong() : null);

        Date scheduleDate = localInstance.getScheduleDate();
        TimePair scheduleTimePair = localInstance.getScheduleTimePair();

        String scheduleRemoteCustomTimeId;
        Integer scheduleHour;
        Integer scheduleMinute;
        if (scheduleTimePair.mHourMinute != null) {
            Assert.assertTrue(scheduleTimePair.mCustomTimeKey == null);

            scheduleRemoteCustomTimeId = null;

            scheduleHour = scheduleTimePair.mHourMinute.getHour();
            scheduleMinute = scheduleTimePair.mHourMinute.getMinute();
        } else {
            Assert.assertTrue(scheduleTimePair.mCustomTimeKey != null);

            scheduleRemoteCustomTimeId = getRemoteCustomTimeId(scheduleTimePair.mCustomTimeKey, recordOf);

            scheduleHour = null;
            scheduleMinute = null;
        }

        Date instanceDate = localInstance.getInstanceDate();
        TimePair instanceTimePair = localInstance.getInstanceTimePair();

        String instanceRemoteCustomTimeId;
        Integer instanceHour;
        Integer instanceMinute;
        if (instanceTimePair.mHourMinute != null) {
            Assert.assertTrue(instanceTimePair.mCustomTimeKey == null);

            instanceRemoteCustomTimeId = null;

            instanceHour = instanceTimePair.mHourMinute.getHour();
            instanceMinute = instanceTimePair.mHourMinute.getMinute();
        } else {
            Assert.assertTrue(instanceTimePair.mCustomTimeKey != null);

            instanceRemoteCustomTimeId = getRemoteCustomTimeId(instanceTimePair.mCustomTimeKey, recordOf);

            instanceHour = null;
            instanceMinute = null;
        }

        InstanceJson instanceJson = new InstanceJson(remoteTaskId, done, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), scheduleRemoteCustomTimeId, scheduleHour, scheduleMinute, instanceDate.getYear(), instanceDate.getMonth(), instanceDate.getDay(), instanceRemoteCustomTimeId, instanceHour, instanceMinute, localInstance.getHierarchyTime());
        RemoteInstanceRecord remoteInstanceRecord = new RemoteInstanceRecord(new JsonWrapper(recordOf, instanceJson));

        InstanceShownRecord instanceShownRecord;
        if (localInstance.getNotificationShown() || localInstance.getNotified()) {
            instanceShownRecord = mDomainFactory.getLocalFactory().createInstanceShownRecord(mDomainFactory, remoteTaskId, localInstance.getScheduleDateTime());
        } else {
            instanceShownRecord = null;
        }

        RemoteInstance remoteInstance = new RemoteInstance(mDomainFactory, remoteInstanceRecord, instanceShownRecord);
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

    @NonNull
    private String getRemoteCustomTimeId(@NonNull CustomTimeKey customTimeKey, @NonNull Set<String> recordOf) {
        Assert.assertTrue(customTimeKey.mLocalCustomTimeId != null);
        Assert.assertTrue(TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId));

        int localCustomTimeId = customTimeKey.mLocalCustomTimeId;

        Assert.assertTrue(mDomainFactory.getLocalFactory().mLocalCustomTimes.containsKey(localCustomTimeId));
        LocalCustomTime localCustomTime = mDomainFactory.getLocalFactory().mLocalCustomTimes.get(localCustomTimeId);

        if (!localCustomTime.hasRemoteRecord()) {
            CustomTimeJson customTimeJson = new CustomTimeJson(UserData.getKey(mUserData.email), localCustomTime.getId(), localCustomTime.getName(), localCustomTime.getHourMinute(DayOfWeek.SUNDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.SUNDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.MONDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.MONDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.TUESDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.TUESDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.THURSDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.THURSDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.FRIDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.FRIDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.SATURDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.SATURDAY).getMinute());
            JsonWrapper jsonWrapper = new JsonWrapper(recordOf, customTimeJson);

            RemoteCustomTimeRecord remoteCustomTimeRecord = mRemoteManager.newRemoteCustomTimeRecord(jsonWrapper);

            localCustomTime.setRemoteCustomTimeRecord(remoteCustomTimeRecord);
        } else {
            localCustomTime.updateRecordOf(recordOf, new HashSet<>());
        }

        return localCustomTime.getRemoteId();
    }
}
