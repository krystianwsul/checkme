package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteFactory {
    @NonNull
    private final RemoteManager mRemoteManager;

    @NonNull
    public final Map<String, RemoteTask> mRemoteTasks;

    @NonNull
    public final Map<String, RemoteTaskHierarchy> mRemoteTaskHierarchies;

    @NonNull
    final Multimap<String, Schedule> mRemoteSchedules;

    @NonNull
    public final Map<String, RemoteInstance> mExistingRemoteInstances;

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

    public RemoteTask createScheduleRootTask(@NonNull DomainFactory domainFactory, @NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull List<UserData> friendEntries) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note);

        UserData userData = MainActivity.getUserData();
        Assert.assertTrue(userData != null);

        List<UserData> userDatas = new ArrayList<>(friendEntries);
        userDatas.add(userData);

        RemoteTaskRecord remoteTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(userDatas, taskJson));
        String taskId = remoteTaskRecord.getId();

        for (CreateTaskLoader.ScheduleData scheduleData : scheduleDatas) {
            Assert.assertTrue(scheduleData != null);

            switch (scheduleData.getScheduleType()) {
                case SINGLE: {
                    CreateTaskLoader.SingleScheduleData singleScheduleData = (CreateTaskLoader.SingleScheduleData) scheduleData;

                    Date date = singleScheduleData.Date;

                    Assert.assertTrue(singleScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = singleScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteSingleScheduleRecord remoteSingleScheduleRecord = mRemoteManager.newRemoteSingleScheduleRecord(new JsonWrapper(userDatas, new SingleScheduleJson(taskId, now.getLong(), null, date.getYear(), date.getMonth(), date.getDay(), null, hourMinute.getHour(), hourMinute.getMinute())));
                    Assert.assertTrue(!mRemoteSchedules.containsKey(remoteSingleScheduleRecord.getId()));

                    mRemoteSchedules.put(remoteSingleScheduleRecord.getId(), new SingleSchedule(domainFactory, new RemoteSingleScheduleBridge(remoteSingleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                    Assert.assertTrue(dailyScheduleData.TimePair.mCustomTimeId == null); // todo custom time

                    HourMinute hourMinute = dailyScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteDailyScheduleRecord remoteDailyScheduleRecord = mRemoteManager.newRemoteDailyScheduleRecord(new JsonWrapper(userDatas, new DailyScheduleJson(taskId, now.getLong(), null, null, hourMinute.getHour(), hourMinute.getMinute())));
                    Assert.assertTrue(!mRemoteSchedules.containsKey(remoteDailyScheduleRecord.getId()));

                    mRemoteSchedules.put(remoteDailyScheduleRecord.getId(), new DailySchedule(domainFactory, new RemoteDailyScheduleBridge(remoteDailyScheduleRecord)));
                    break;
                }
                case WEEKLY: {
                    CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;

                    DayOfWeek dayOfWeek = weeklyScheduleData.DayOfWeek;

                    Assert.assertTrue(weeklyScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = weeklyScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = mRemoteManager.newRemoteWeeklyScheduleRecord(new JsonWrapper(userDatas, new WeeklyScheduleJson(taskId, now.getLong(), null, dayOfWeek.ordinal(), null, hourMinute.getHour(), hourMinute.getMinute())));
                    Assert.assertTrue(!mRemoteSchedules.containsKey(remoteWeeklyScheduleRecord.getId()));

                    mRemoteSchedules.put(remoteWeeklyScheduleRecord.getId(), new WeeklySchedule(domainFactory, new RemoteWeeklyScheduleBridge(remoteWeeklyScheduleRecord)));
                    break;
                }
                case MONTHLY_DAY: {
                    CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;

                    Assert.assertTrue(monthlyDayScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = monthlyDayScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = mRemoteManager.newRemoteMonthlyDayScheduleRecord(new JsonWrapper(userDatas, new MonthlyDayScheduleJson(taskId, now.getLong(), null, monthlyDayScheduleData.mDayOfMonth, monthlyDayScheduleData.mBeginningOfMonth, null, hourMinute.getHour(), hourMinute.getMinute())));
                    Assert.assertTrue(!mRemoteSchedules.containsKey(remoteMonthlyDayScheduleRecord.getId()));

                    mRemoteSchedules.put(remoteMonthlyDayScheduleRecord.getId(), new MonthlyDaySchedule(domainFactory, new RemoteMonthlyDayScheduleBridge(remoteMonthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;

                    Assert.assertTrue(monthlyWeekScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = monthlyWeekScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = mRemoteManager.newRemoteMonthlyWeekScheduleRecord(new JsonWrapper(userDatas, new MonthlyWeekScheduleJson(taskId, now.getLong(), null, monthlyWeekScheduleData.mDayOfMonth, monthlyWeekScheduleData.mDayOfWeek.ordinal(), monthlyWeekScheduleData.mBeginningOfMonth, null, hourMinute.getHour(), hourMinute.getMinute())));
                    Assert.assertTrue(!mRemoteSchedules.containsKey(remoteMonthlyWeekScheduleRecord.getId()));

                    mRemoteSchedules.put(remoteMonthlyWeekScheduleRecord.getId(), new MonthlyWeekSchedule(domainFactory, new RemoteMonthlyWeekScheduleBridge(remoteMonthlyWeekScheduleRecord)));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        RemoteTask remoteTask = new RemoteTask(domainFactory, remoteTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));
        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        mRemoteManager.save();

        return remoteTask;
    }

    public void save() {
        mRemoteManager.save();
    }

    RemoteTask createChildTask(@NonNull DomainFactory domainFactory, @NonNull RemoteTask parentTask, @NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note);
        RemoteTaskRecord childTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(parentTask.getRecordOf(), taskJson));

        RemoteTask childTask = new RemoteTask(domainFactory, childTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(childTask.getId()));

        mRemoteTasks.put(childTask.getId(), childTask);

        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(parentTask.getId(), childTaskRecord.getId(), now.getLong(), null);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteManager.newRemoteTaskHierarchyRecord(new JsonWrapper(parentTask.getRecordOf(), taskHierarchyJson));

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(domainFactory, remoteTaskHierarchyRecord);
        Assert.assertTrue(!mRemoteTaskHierarchies.containsKey(remoteTaskHierarchy.getId()));

        mRemoteTaskHierarchies.put(remoteTaskHierarchy.getId(), remoteTaskHierarchy);

        return childTask;
    }

    public RemoteInstanceRecord createRemoteInstanceRecord(@NonNull RemoteTask remoteTask, @NonNull RemoteInstance remoteInstance, @NonNull DateTime scheduleDateTime, @NonNull ExactTimeStamp now) {
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
}
