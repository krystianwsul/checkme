package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.json.DailyScheduleJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.MonthlyDayScheduleJson;
import com.krystianwsul.checkme.firebase.json.MonthlyWeekScheduleJson;
import com.krystianwsul.checkme.firebase.json.SingleScheduleJson;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.firebase.json.WeeklyScheduleJson;
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteManager;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyWeekScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteSingleScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
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
    final Multimap<String, RemoteSchedule> mRemoteSchedules;

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
            mRemoteSchedules.put(remoteSingleScheduleRecord.getTaskId(), new RemoteSingleSchedule(remoteSingleScheduleRecord));

        for (RemoteDailyScheduleRecord remoteDailyScheduleRecord : mRemoteManager.mRemoteDailyScheduleRecords.values())
            mRemoteSchedules.put(remoteDailyScheduleRecord.getTaskId(), new RemoteDailySchedule(remoteDailyScheduleRecord));

        for (RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord : mRemoteManager.mRemoteWeeklyScheduleRecords.values())
            mRemoteSchedules.put(remoteWeeklyScheduleRecord.getTaskId(), new RemoteWeeklySchedule(remoteWeeklyScheduleRecord));

        for (RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord : mRemoteManager.mRemoteMonthlyDayScheduleRecords.values())
            mRemoteSchedules.put(remoteMonthlyDayScheduleRecord.getTaskId(), new RemoteMonthlyDaySchedule(remoteMonthlyDayScheduleRecord));

        for (RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord : mRemoteManager.mRemoteMonthlyWeekScheduleRecords.values())
            mRemoteSchedules.put(remoteMonthlyWeekScheduleRecord.getTaskId(), new RemoteMonthlyWeekSchedule(remoteMonthlyWeekScheduleRecord));
    }

    public void createScheduleRootTask(@NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull List<UserData> friendEntries) {
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

                    mRemoteManager.newRemoteSingleScheduleRecord(new JsonWrapper(userDatas, new SingleScheduleJson(taskId, now.getLong(), null, date.getYear(), date.getMonth(), date.getDay(), null, hourMinute.getHour(), hourMinute.getMinute())));
                    break;
                }
                case DAILY: {
                    CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                    Assert.assertTrue(dailyScheduleData.TimePair.mCustomTimeId == null); // todo custom time

                    HourMinute hourMinute = dailyScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    mRemoteManager.newRemoteDailyScheduleRecord(new JsonWrapper(userDatas, new DailyScheduleJson(taskId, now.getLong(), null, null, hourMinute.getHour(), hourMinute.getMinute())));
                    break;
                }
                case WEEKLY: {
                    CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;

                    DayOfWeek dayOfWeek = weeklyScheduleData.DayOfWeek;

                    Assert.assertTrue(weeklyScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = weeklyScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    mRemoteManager.newRemoteWeeklyScheduleRecord(new JsonWrapper(userDatas, new WeeklyScheduleJson(taskId, now.getLong(), null, dayOfWeek.ordinal(), null, hourMinute.getHour(), hourMinute.getMinute())));
                    break;
                }
                case MONTHLY_DAY: {
                    CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;

                    Assert.assertTrue(monthlyDayScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = monthlyDayScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    mRemoteManager.newRemoteMonthlyDayScheduleRecord(new JsonWrapper(userDatas, new MonthlyDayScheduleJson(taskId, now.getLong(), null, monthlyDayScheduleData.mDayOfMonth, monthlyDayScheduleData.mBeginningOfMonth, null, hourMinute.getHour(), hourMinute.getMinute())));
                    break;
                }
                case MONTHLY_WEEK: {
                    CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;

                    Assert.assertTrue(monthlyWeekScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                    HourMinute hourMinute = monthlyWeekScheduleData.TimePair.mHourMinute;
                    Assert.assertTrue(hourMinute != null);

                    mRemoteManager.newRemoteMonthlyWeekScheduleRecord(new JsonWrapper(userDatas, new MonthlyWeekScheduleJson(taskId, now.getLong(), null, monthlyWeekScheduleData.mDayOfMonth, monthlyWeekScheduleData.mDayOfWeek.ordinal(), monthlyWeekScheduleData.mBeginningOfMonth, null, hourMinute.getHour(), hourMinute.getMinute())));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        mRemoteManager.save();
    }

    public void save() {
        mRemoteManager.save();
    }

    void createChildTask(@NonNull RemoteTask parentTask, @NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note);
        RemoteTaskRecord childTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(parentTask.getRecordOf(), taskJson));

        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(parentTask.getId(), childTaskRecord.getId(), now.getLong(), null);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteManager.newRemoteTaskHierarchyRecord(new JsonWrapper(parentTask.getRecordOf(), taskHierarchyJson));
    }
}
