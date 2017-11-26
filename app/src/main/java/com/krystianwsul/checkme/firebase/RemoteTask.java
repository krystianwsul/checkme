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
import com.krystianwsul.checkme.utils.time.TimePair;

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

    RemoteTask(@NonNull DomainFactory domainFactory, @NonNull RemoteProject remoteProject, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ExactTimeStamp now) {
        super(domainFactory);

        mRemoteProject = remoteProject;
        mRemoteTaskRecord = remoteTaskRecord;

        mExistingRemoteInstances = Stream.of(mRemoteTaskRecord.getRemoteInstanceRecords().values())
                .map(remoteInstanceRecord -> new RemoteInstance(domainFactory, mRemoteProject, remoteInstanceRecord, domainFactory.getLocalFactory().getInstanceShownRecord(mRemoteProject.getId(), remoteInstanceRecord.getTaskId(), remoteInstanceRecord.getScheduleYear(), remoteInstanceRecord.getScheduleMonth(), remoteInstanceRecord.getScheduleDay(), remoteInstanceRecord.getScheduleCustomTimeId(), remoteInstanceRecord.getScheduleHour(), remoteInstanceRecord.getScheduleMinute()), now))
                .collect(Collectors.toMap(RemoteInstance::getScheduleKey, remoteInstance -> remoteInstance));

        mRemoteSchedules.addAll(Stream.of(mRemoteTaskRecord.mRemoteSingleScheduleRecords.values())
                .map(remoteSingleScheduleRecord -> new SingleSchedule(domainFactory, new RemoteSingleScheduleBridge(domainFactory, remoteSingleScheduleRecord)))
                .collect(Collectors.toList()));

        mRemoteSchedules.addAll(Stream.of(mRemoteTaskRecord.mRemoteDailyScheduleRecords.values())
                .map(remoteDailyScheduleRecord -> new DailySchedule(domainFactory, new RemoteDailyScheduleBridge(domainFactory, remoteDailyScheduleRecord)))
                .collect(Collectors.toList()));

        mRemoteSchedules.addAll(Stream.of(mRemoteTaskRecord.mRemoteWeeklyScheduleRecords.values())
                .map(remoteWeeklyScheduleRecord -> new WeeklySchedule(domainFactory, new RemoteWeeklyScheduleBridge(domainFactory, remoteWeeklyScheduleRecord)))
                .collect(Collectors.toList()));

        mRemoteSchedules.addAll(Stream.of(mRemoteTaskRecord.mRemoteMonthlyDayScheduleRecords.values())
                .map(remoteMonthlyDayScheduleRecord -> new MonthlyDaySchedule(domainFactory, new RemoteMonthlyDayScheduleBridge(domainFactory, remoteMonthlyDayScheduleRecord)))
                .collect(Collectors.toList()));

        mRemoteSchedules.addAll(Stream.of(mRemoteTaskRecord.mRemoteMonthlyWeekScheduleRecords.values())
                .map(remoteMonthlyWeekScheduleRecord -> new MonthlyWeekSchedule(domainFactory, new RemoteMonthlyWeekScheduleBridge(domainFactory, remoteMonthlyWeekScheduleRecord)))
                .collect(Collectors.toList()));
    }

    @NonNull
    @Override
    public String getName() {
        return mRemoteTaskRecord.getName();
    }

    @NonNull
    private RemoteProjectFactory getRemoteFactory() {
        RemoteProjectFactory remoteProjectFactory = mDomainFactory.getRemoteFactory();
        Assert.assertTrue(remoteProjectFactory != null);

        return remoteProjectFactory;
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

        RemoteTask childTask = mRemoteProject.newRemoteTask(taskJson, now);

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

        Stream.of(new ArrayList<>(getRemoteFactory().getTaskHierarchiesByChildTaskKey(taskKey)))
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
                    CreateTaskLoader.ScheduleData.SingleScheduleData singleScheduleData = (CreateTaskLoader.ScheduleData.SingleScheduleData) scheduleData;

                    Date date = singleScheduleData.getDate();

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (singleScheduleData.getTimePair().mCustomTimeKey != null) {
                        Assert.assertTrue(singleScheduleData.getTimePair().mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(singleScheduleData.getTimePair().mCustomTimeKey, mRemoteProject);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(singleScheduleData.getTimePair().mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = singleScheduleData.getTimePair().mHourMinute.getHour();
                        minute = singleScheduleData.getTimePair().mHourMinute.getMinute();
                    }

                    RemoteSingleScheduleRecord remoteSingleScheduleRecord = mRemoteTaskRecord.newRemoteSingleScheduleRecord(new ScheduleWrapper(new SingleScheduleJson(now.getLong(), null, date.getYear(), date.getMonth(), date.getDay(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new SingleSchedule(mDomainFactory, new RemoteSingleScheduleBridge(mDomainFactory, remoteSingleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    throw new UnsupportedOperationException();
                }
                case WEEKLY: {
                    CreateTaskLoader.ScheduleData.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.ScheduleData.WeeklyScheduleData) scheduleData;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (weeklyScheduleData.getTimePair().mCustomTimeKey != null) {
                        Assert.assertTrue(weeklyScheduleData.getTimePair().mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(weeklyScheduleData.getTimePair().mCustomTimeKey, mRemoteProject);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(weeklyScheduleData.getTimePair().mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = weeklyScheduleData.getTimePair().mHourMinute.getHour();
                        minute = weeklyScheduleData.getTimePair().mHourMinute.getMinute();
                    }

                    for (DayOfWeek dayOfWeek : weeklyScheduleData.getDaysOfWeek()) {
                        RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = mRemoteTaskRecord.newRemoteWeeklyScheduleRecord(new ScheduleWrapper(new WeeklyScheduleJson(now.getLong(), null, dayOfWeek.ordinal(), remoteCustomTimeId, hour, minute)));

                        mRemoteSchedules.add(new WeeklySchedule(mDomainFactory, new RemoteWeeklyScheduleBridge(mDomainFactory, remoteWeeklyScheduleRecord)));
                    }

                    break;
                }
                case MONTHLY_DAY: {
                    CreateTaskLoader.ScheduleData.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.ScheduleData.MonthlyDayScheduleData) scheduleData;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (monthlyDayScheduleData.getTimePair().mCustomTimeKey != null) {
                        Assert.assertTrue(monthlyDayScheduleData.getTimePair().mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(monthlyDayScheduleData.getTimePair().mCustomTimeKey, mRemoteProject);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(monthlyDayScheduleData.getTimePair().mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = monthlyDayScheduleData.getTimePair().mHourMinute.getHour();
                        minute = monthlyDayScheduleData.getTimePair().mHourMinute.getMinute();
                    }

                    RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = mRemoteTaskRecord.newRemoteMonthlyDayScheduleRecord(new ScheduleWrapper(new MonthlyDayScheduleJson(now.getLong(), null, monthlyDayScheduleData.getDayOfMonth(), monthlyDayScheduleData.getBeginningOfMonth(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new MonthlyDaySchedule(mDomainFactory, new RemoteMonthlyDayScheduleBridge(mDomainFactory, remoteMonthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData) scheduleData;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;
                    if (monthlyWeekScheduleData.getTimePair().mCustomTimeKey != null) {
                        Assert.assertTrue(monthlyWeekScheduleData.getTimePair().mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(monthlyWeekScheduleData.getTimePair().mCustomTimeKey, mRemoteProject);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(monthlyWeekScheduleData.getTimePair().mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = monthlyWeekScheduleData.getTimePair().mHourMinute.getHour();
                        minute = monthlyWeekScheduleData.getTimePair().mHourMinute.getMinute();
                    }

                    RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = mRemoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(new ScheduleWrapper(new MonthlyWeekScheduleJson(now.getLong(), null, monthlyWeekScheduleData.getDayOfMonth(), monthlyWeekScheduleData.getDayOfWeek().ordinal(), monthlyWeekScheduleData.getBeginningOfMonth(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new MonthlyWeekSchedule(mDomainFactory, new RemoteMonthlyWeekScheduleBridge(mDomainFactory, remoteMonthlyWeekScheduleRecord)));
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    void copySchedules(@NonNull Collection<Schedule> schedules) {
        for (Schedule schedule : schedules) {
            Assert.assertTrue(schedule != null);

            switch (schedule.getScheduleType()) {
                case SINGLE: {
                    SingleSchedule singleSchedule = (SingleSchedule) schedule;

                    Date date = singleSchedule.getDate();

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;

                    TimePair timePair = singleSchedule.getTimePair();
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(timePair.mCustomTimeKey, mRemoteProject);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = timePair.mHourMinute.getHour();
                        minute = timePair.mHourMinute.getMinute();
                    }

                    RemoteSingleScheduleRecord remoteSingleScheduleRecord = mRemoteTaskRecord.newRemoteSingleScheduleRecord(new ScheduleWrapper(new SingleScheduleJson(singleSchedule.getStartTime(), singleSchedule.getEndTime(), date.getYear(), date.getMonth(), date.getDay(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new SingleSchedule(mDomainFactory, new RemoteSingleScheduleBridge(mDomainFactory, remoteSingleScheduleRecord)));
                    break;
                }
                case DAILY: {
                    DailySchedule dailySchedule = (DailySchedule) schedule;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;

                    TimePair timePair = dailySchedule.getTimePair();
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(timePair.mCustomTimeKey, mRemoteProject);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = timePair.mHourMinute.getHour();
                        minute = timePair.mHourMinute.getMinute();
                    }

                    RemoteDailyScheduleRecord remoteDailyScheduleRecord = mRemoteTaskRecord.newRemoteDailyScheduleRecord(new ScheduleWrapper(new DailyScheduleJson(schedule.getStartTime(), schedule.getEndTime(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new DailySchedule(mDomainFactory, new RemoteDailyScheduleBridge(mDomainFactory, remoteDailyScheduleRecord)));
                    break;
                }
                case WEEKLY: {
                    WeeklySchedule weeklySchedule = (WeeklySchedule) schedule;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;

                    TimePair timePair = weeklySchedule.getTimePair();
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(timePair.mCustomTimeKey, mRemoteProject);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = timePair.mHourMinute.getHour();
                        minute = timePair.mHourMinute.getMinute();
                    }

                    for (DayOfWeek dayOfWeek : weeklySchedule.getDaysOfWeek()) {
                        RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = mRemoteTaskRecord.newRemoteWeeklyScheduleRecord(new ScheduleWrapper(new WeeklyScheduleJson(schedule.getStartTime(), schedule.getEndTime(), dayOfWeek.ordinal(), remoteCustomTimeId, hour, minute)));

                        mRemoteSchedules.add(new WeeklySchedule(mDomainFactory, new RemoteWeeklyScheduleBridge(mDomainFactory, remoteWeeklyScheduleRecord)));
                    }

                    break;
                }
                case MONTHLY_DAY: {
                    MonthlyDaySchedule monthlyDaySchedule = (MonthlyDaySchedule) schedule;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;

                    TimePair timePair = monthlyDaySchedule.getTimePair();
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(timePair.mCustomTimeKey, mRemoteProject);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = timePair.mHourMinute.getHour();
                        minute = timePair.mHourMinute.getMinute();
                    }

                    RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = mRemoteTaskRecord.newRemoteMonthlyDayScheduleRecord(new ScheduleWrapper(new MonthlyDayScheduleJson(schedule.getStartTime(), schedule.getEndTime(), monthlyDaySchedule.getDayOfMonth(), monthlyDaySchedule.getBeginningOfMonth(), remoteCustomTimeId, hour, minute)));

                    mRemoteSchedules.add(new MonthlyDaySchedule(mDomainFactory, new RemoteMonthlyDayScheduleBridge(mDomainFactory, remoteMonthlyDayScheduleRecord)));
                    break;
                }
                case MONTHLY_WEEK: {
                    MonthlyWeekSchedule monthlyWeekScheduleData = (MonthlyWeekSchedule) schedule;

                    String remoteCustomTimeId;
                    Integer hour;
                    Integer minute;

                    TimePair timePair = monthlyWeekScheduleData.getTimePair();
                    if (timePair.mCustomTimeKey != null) {
                        Assert.assertTrue(timePair.mHourMinute == null);

                        remoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(timePair.mCustomTimeKey, mRemoteProject);
                        hour = null;
                        minute = null;
                    } else {
                        Assert.assertTrue(timePair.mHourMinute != null);

                        remoteCustomTimeId = null;
                        hour = timePair.mHourMinute.getHour();
                        minute = timePair.mHourMinute.getMinute();
                    }

                    RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = mRemoteTaskRecord.newRemoteMonthlyWeekScheduleRecord(new ScheduleWrapper(new MonthlyWeekScheduleJson(schedule.getStartTime(), schedule.getEndTime(), monthlyWeekScheduleData.getDayOfMonth(), monthlyWeekScheduleData.getDayOfWeek().ordinal(), monthlyWeekScheduleData.getBeginningOfMonth(), remoteCustomTimeId, hour, minute)));

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

    @NonNull
    @Override
    public RemoteTask updateProject(@NonNull Context context, @NonNull ExactTimeStamp now, @Nullable String projectId) {
        Assert.assertTrue(TextUtils.isEmpty(projectId));

        return this;
    }
}
