package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DailySchedule;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MonthlyDaySchedule;
import com.krystianwsul.checkme.domainmodel.MonthlyWeekSchedule;
import com.krystianwsul.checkme.domainmodel.Schedule;
import com.krystianwsul.checkme.domainmodel.SingleSchedule;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.domainmodel.TaskHierarchy;
import com.krystianwsul.checkme.domainmodel.WeeklySchedule;
import com.krystianwsul.checkme.firebase.json.DailyScheduleJson;
import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.firebase.json.MonthlyDayScheduleJson;
import com.krystianwsul.checkme.firebase.json.MonthlyWeekScheduleJson;
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper;
import com.krystianwsul.checkme.firebase.json.SingleScheduleJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.firebase.json.WeeklyScheduleJson;
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyWeekScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteSingleScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoteTask extends Task {
    @NonNull
    private final RemoteProject mRemoteProject;

    @NonNull
    private final RemoteTaskRecord mRemoteTaskRecord;

    @NonNull
    private final Map<ScheduleKey, RemoteInstance> mExistingRemoteInstances;

    @NonNull
    private final List<Schedule> mRemoteSchedules = new ArrayList<>();

    RemoteTask(@NonNull DomainFactory domainFactory, @NonNull RemoteProject remoteProject, @NonNull RemoteTaskRecord remoteTaskRecord) {
        super(domainFactory);

        mRemoteProject = remoteProject;
        mRemoteTaskRecord = remoteTaskRecord;

        mExistingRemoteInstances = Stream.of(mRemoteTaskRecord.getRemoteInstanceRecords().values())
                .map(remoteInstanceRecord -> new RemoteInstance(domainFactory, mRemoteProject, remoteInstanceRecord, domainFactory.getLocalFactory().getInstanceShownRecord(remoteInstanceRecord.getTaskId(), remoteInstanceRecord.getScheduleYear(), remoteInstanceRecord.getScheduleMonth(), remoteInstanceRecord.getScheduleDay(), remoteInstanceRecord.getScheduleCustomTimeId(), remoteInstanceRecord.getScheduleHour(), remoteInstanceRecord.getScheduleMinute())))
                .collect(Collectors.toMap(RemoteInstance::getScheduleKey, remoteInstance -> remoteInstance));

        for (RemoteSingleScheduleRecord remoteSingleScheduleRecord : mRemoteTaskRecord.mRemoteSingleScheduleRecords.values())
            mRemoteSchedules.add(new SingleSchedule(domainFactory, new RemoteSingleScheduleBridge(domainFactory, remoteSingleScheduleRecord)));

        for (RemoteDailyScheduleRecord remoteDailyScheduleRecord : mRemoteTaskRecord.mRemoteDailyScheduleRecords.values())
            mRemoteSchedules.add(new DailySchedule(domainFactory, new RemoteDailyScheduleBridge(domainFactory, remoteDailyScheduleRecord)));

        for (RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord : mRemoteTaskRecord.mRemoteWeeklyScheduleRecords.values())
            mRemoteSchedules.add(new WeeklySchedule(domainFactory, new RemoteWeeklyScheduleBridge(domainFactory, remoteWeeklyScheduleRecord)));

        for (RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord : mRemoteTaskRecord.mRemoteMonthlyDayScheduleRecords.values())
            mRemoteSchedules.add(new MonthlyDaySchedule(domainFactory, new RemoteMonthlyDayScheduleBridge(domainFactory, remoteMonthlyDayScheduleRecord)));

        for (RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord : mRemoteTaskRecord.mRemoteMonthlyWeekScheduleRecords.values())
            mRemoteSchedules.add(new MonthlyWeekSchedule(domainFactory, new RemoteMonthlyWeekScheduleBridge(domainFactory, remoteMonthlyWeekScheduleRecord)));
    }

    @NonNull
    @Override
    public String getName() {
        return mRemoteTaskRecord.getName();
    }

    @NonNull
    private RemoteFactory getRemoteFactory() {
        RemoteFactory remoteFactory = mDomainFactory.getRemoteFactory();
        Assert.assertTrue(remoteFactory != null);

        return remoteFactory;
    }

    @NonNull
    @Override
    protected Collection<Schedule> getSchedules() {
        return mRemoteSchedules;
    }

    @NonNull
    @Override
    public ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mRemoteTaskRecord.getStartTime());
    }

    @Nullable
    @Override
    public ExactTimeStamp getEndExactTimeStamp() {
        if (mRemoteTaskRecord.getEndTime() != null)
            return new ExactTimeStamp(mRemoteTaskRecord.getEndTime());
        else
            return null;
    }

    @Nullable
    @Override
    public String getNote() {
        return mRemoteTaskRecord.getNote();
    }

    @NonNull
    @Override
    public TaskKey getTaskKey() {
        return new TaskKey(mRemoteProject.getId(), mRemoteTaskRecord.getId());
    }

    @NonNull
    @Override
    public Set<String> getRecordOf() {
        return mRemoteProject.getRecordOf();
    }

    @Override
    protected void setMyEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        mRemoteTaskRecord.setEndTime(now.getLong());
    }

    @NonNull
    public String getId() {
        return mRemoteTaskRecord.getId();
    }

    @NonNull
    @Override
    public Task createChildTask(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note, Collections.emptyMap());

        RemoteTask childTask = mRemoteProject.newRemoteTask(taskJson);

        mRemoteProject.createTaskHierarchy(this, childTask, now);

        return childTask;
    }

    @Nullable
    @Override
    public Date getOldestVisible() {
        if (mRemoteTaskRecord.getOldestVisibleYear() != null) {
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleMonth() != null);
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleDay() != null);

            return new Date(mRemoteTaskRecord.getOldestVisibleYear(), mRemoteTaskRecord.getOldestVisibleMonth(), mRemoteTaskRecord.getOldestVisibleDay());
        } else {
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleMonth() == null);
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleDay() == null);

            return null;
        }
    }

    @Override
    protected void setOldestVisible(@NonNull Date date) {
        mRemoteTaskRecord.setOldestVisibleYear(date.getYear());
        mRemoteTaskRecord.setOldestVisibleMonth(date.getMonth());
        mRemoteTaskRecord.setOldestVisibleDay(date.getDay());
    }

    @Override
    public void delete() {
        TaskKey taskKey = getTaskKey();

        Stream.of(getRemoteFactory().getTaskHierarchiesByChildTaskKey(taskKey))
                .forEach(TaskHierarchy::delete);

        Stream.of(new ArrayList<>(getSchedules()))
                .forEach(Schedule::delete);

        mRemoteProject.deleteTask(this);
        mRemoteTaskRecord.delete();
    }

    @Override
    public void setName(@NonNull String name, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mRemoteTaskRecord.setName(name);
        mRemoteTaskRecord.setNote(note);
    }

    @NonNull
    @Override
    protected Task updateFriends(@NonNull Set<String> friends, @NonNull Context context, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(mDomainFactory.getFriends() != null);

        UserData userData = getRemoteFactory().getUserData();

        String myKey = userData.getKey();
        Assert.assertTrue(!friends.contains(myKey));

        Set<String> allFriends = mDomainFactory.getFriends().keySet();
        Assert.assertTrue(!allFriends.contains(myKey));

        Set<String> oldFriends = Stream.of(getRecordOf())
                .filter(allFriends::contains)
                .filterNot(myKey::equals)
                .collect(Collectors.toSet());

        Set<String> addedFriends = Stream.of(friends)
                .filterNot(oldFriends::contains)
                .collect(Collectors.toSet());
        Assert.assertTrue(!addedFriends.contains(myKey));

        Set<String> removedFriends = Stream.of(oldFriends)
                .filterNot(friends::contains)
                .collect(Collectors.toSet());
        Assert.assertTrue(!removedFriends.contains(myKey));

        mRemoteProject.updateRecordOf(addedFriends, removedFriends);

        return this;
    }

    @Override
    protected void addSchedules(@NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull ExactTimeStamp now) {
        createSchedules(now, scheduleDatas);
    }

    @Override
    public void addChild(@NonNull Task childTask, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(childTask instanceof RemoteTask);

        mRemoteProject.createTaskHierarchy(this, (RemoteTask) childTask, now);
    }

    @Override
    protected void deleteSchedule(@NonNull Schedule schedule) {
        Assert.assertTrue(mRemoteSchedules.contains(schedule));

        mRemoteSchedules.remove(schedule);
    }

    @NonNull
    RemoteInstanceRecord createRemoteInstanceRecord(@NonNull RemoteInstance remoteInstance, @NonNull DateTime scheduleDateTime, @NonNull ExactTimeStamp now) {
        InstanceJson instanceJson = new InstanceJson(null, null, null, null, null, null, null, now.getLong());

        ScheduleKey scheduleKey = new ScheduleKey(scheduleDateTime.getDate(), scheduleDateTime.getTime().getTimePair());

        RemoteInstanceRecord remoteInstanceRecord = mRemoteTaskRecord.newRemoteInstanceRecord(mDomainFactory, instanceJson, scheduleKey);

        mExistingRemoteInstances.put(remoteInstance.getScheduleKey(), remoteInstance);

        return remoteInstanceRecord;
    }

    @Override
    @NonNull
    public Map<ScheduleKey, RemoteInstance> getExistingInstances() {
        return mExistingRemoteInstances;
    }

    void deleteInstance(@NonNull RemoteInstance remoteInstance) {
        ScheduleKey scheduleKey = remoteInstance.getScheduleKey();

        Assert.assertTrue(mExistingRemoteInstances.containsKey(scheduleKey));
        Assert.assertTrue(remoteInstance.equals(mExistingRemoteInstances.get(scheduleKey)));

        mExistingRemoteInstances.remove(scheduleKey);
    }

    @Nullable
    RemoteInstance getExistingInstanceIfPresent(@NonNull ScheduleKey scheduleKey) {
        return mExistingRemoteInstances.get(scheduleKey);
    }

    void createSchedules(@NonNull ExactTimeStamp now, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas) {
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

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(singleScheduleData.TimePair.mCustomTimeKey, getRecordOf());
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(singleScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = singleScheduleData.TimePair.mHourMinute.getHour();
                        minute = singleScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteSingleScheduleRecord remoteSingleScheduleRecord = mRemoteTaskRecord.newRemoteSingleScheduleRecord(new ScheduleWrapper(new SingleScheduleJson(now.getLong(), null, date.getYear(), date.getMonth(), date.getDay(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new SingleSchedule(mDomainFactory, new RemoteSingleScheduleBridge(mDomainFactory, remoteSingleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (dailyScheduleData.TimePair.mCustomTimeKey != null) {
                        Assert.assertTrue(dailyScheduleData.TimePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(dailyScheduleData.TimePair.mCustomTimeKey, getRecordOf());
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(dailyScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = dailyScheduleData.TimePair.mHourMinute.getHour();
                        minute = dailyScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteDailyScheduleRecord remoteDailyScheduleRecord = mRemoteTaskRecord.newRemoteDailyScheduleRecord(new ScheduleWrapper(new DailyScheduleJson(now.getLong(), null, remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new DailySchedule(mDomainFactory, new RemoteDailyScheduleBridge(mDomainFactory, remoteDailyScheduleRecord)));
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

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(weeklyScheduleData.TimePair.mCustomTimeKey, getRecordOf());
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(weeklyScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = weeklyScheduleData.TimePair.mHourMinute.getHour();
                        minute = weeklyScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = mRemoteTaskRecord.newRemoteWeeklyScheduleRecord(new ScheduleWrapper(new WeeklyScheduleJson(now.getLong(), null, dayOfWeek.ordinal(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new WeeklySchedule(mDomainFactory, new RemoteWeeklyScheduleBridge(mDomainFactory, remoteWeeklyScheduleRecord)));
                    break;
                }
                case MONTHLY_DAY: {
                    CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (monthlyDayScheduleData.TimePair.mCustomTimeKey != null) {
                        Assert.assertTrue(monthlyDayScheduleData.TimePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(monthlyDayScheduleData.TimePair.mCustomTimeKey, getRecordOf());
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(monthlyDayScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = monthlyDayScheduleData.TimePair.mHourMinute.getHour();
                        minute = monthlyDayScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = mRemoteTaskRecord.newRemoteMonthlyDayScheduleRecord(new ScheduleWrapper(new MonthlyDayScheduleJson(now.getLong(), null, monthlyDayScheduleData.mDayOfMonth, monthlyDayScheduleData.mBeginningOfMonth, remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new MonthlyDaySchedule(mDomainFactory, new RemoteMonthlyDayScheduleBridge(mDomainFactory, remoteMonthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (monthlyWeekScheduleData.TimePair.mCustomTimeKey != null) {
                        Assert.assertTrue(monthlyWeekScheduleData.TimePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(monthlyWeekScheduleData.TimePair.mCustomTimeKey, getRecordOf());
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(monthlyWeekScheduleData.TimePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = monthlyWeekScheduleData.TimePair.mHourMinute.getHour();
                        minute = monthlyWeekScheduleData.TimePair.mHourMinute.getMinute();
                    }

                    RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = mRemoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(new ScheduleWrapper(new MonthlyWeekScheduleJson(now.getLong(), null, monthlyWeekScheduleData.mDayOfMonth, monthlyWeekScheduleData.mDayOfWeek.ordinal(), monthlyWeekScheduleData.mBeginningOfMonth, remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new MonthlyWeekSchedule(mDomainFactory, new RemoteMonthlyWeekScheduleBridge(mDomainFactory, remoteMonthlyWeekScheduleRecord)));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    @NonNull
    @Override
    protected Set<? extends TaskHierarchy> getTaskHierarchiesByChildTaskKey(@NonNull TaskKey childTaskKey) {
        return mRemoteProject.getTaskHierarchiesByChildTaskKey(childTaskKey);
    }

    @NonNull
    @Override
    protected Set<? extends TaskHierarchy> getTaskHierarchiesByParentTaskKey(@NonNull TaskKey parentTaskKey) {
        return mRemoteProject.getTaskHierarchiesByParentTaskKey(parentTaskKey);
    }

    @NonNull
    public RemoteProject getRemoteProject() {
        return mRemoteProject;
    }

    @Override
    public boolean belongsToRemoteProject() {
        return true;
    }

    @Nullable
    @Override
    public RemoteProject getRemoteNullableProject() {
        return getRemoteProject();
    }

    @NonNull
    @Override
    public RemoteProject getRemoteNonNullProject() {
        return getRemoteProject();
    }
}
