package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.utils.ScheduleKey;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class RemoteTaskRecord extends RootRemoteRecord {
    public static final String TASK_JSON = "taskJson";

    @NonNull
    private final Map<ScheduleKey, RemoteInstanceRecord> mRemoteInstanceRecords = new HashMap<>();

    @NonNull
    public final Map<String, RemoteSingleScheduleRecord> mRemoteSingleScheduleRecords = new HashMap<>();

    @NonNull
    public final Map<String, RemoteDailyScheduleRecord> mRemoteDailyScheduleRecords = new HashMap<>();

    @NonNull
    public final Map<String, RemoteWeeklyScheduleRecord> mRemoteWeeklyScheduleRecords = new HashMap<>();

    @NonNull
    public final Map<String, RemoteMonthlyDayScheduleRecord> mRemoteMonthlyDayScheduleRecords = new HashMap<>();

    @NonNull
    public final Map<String, RemoteMonthlyWeekScheduleRecord> mRemoteMonthlyWeekScheduleRecords = new HashMap<>();

    RemoteTaskRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);

        initialize();
    }

    RemoteTaskRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);

        initialize();
    }

    private void initialize() {
        for (Map.Entry<String, InstanceJson> entry : getTaskJson().getInstances().entrySet()) {
            InstanceJson instanceJson = entry.getValue();
            Assert.assertTrue(instanceJson != null);

            RemoteInstanceRecord remoteInstanceRecord = new RemoteInstanceRecord(false, this, instanceJson);

            ScheduleKey scheduleKey = remoteInstanceRecord.getScheduleKey();
            Assert.assertTrue(entry.getKey().equals(RemoteInstanceRecord.scheduleKeyToString(scheduleKey)));

            mRemoteInstanceRecords.put(scheduleKey, remoteInstanceRecord);
        }

        for (Map.Entry<String, ScheduleWrapper> entry : getTaskJson().getSchedules().entrySet()) {
            ScheduleWrapper scheduleWrapper = entry.getValue();
            Assert.assertTrue(scheduleWrapper != null);

            String id = entry.getKey();
            Assert.assertTrue(!TextUtils.isEmpty(id));

            if (scheduleWrapper.singleScheduleJson != null) {
                Assert.assertTrue(scheduleWrapper.dailyScheduleJson == null);
                Assert.assertTrue(scheduleWrapper.weeklyScheduleJson == null);
                Assert.assertTrue(scheduleWrapper.monthlyDayScheduleJson == null);
                Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson == null);

                mRemoteSingleScheduleRecords.put(id, new RemoteSingleScheduleRecord(id, this, scheduleWrapper));
            } else if (scheduleWrapper.dailyScheduleJson != null) {
                Assert.assertTrue(scheduleWrapper.weeklyScheduleJson == null);
                Assert.assertTrue(scheduleWrapper.monthlyDayScheduleJson == null);
                Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson == null);

                mRemoteDailyScheduleRecords.put(id, new RemoteDailyScheduleRecord(id, this, scheduleWrapper));
            } else if (scheduleWrapper.weeklyScheduleJson != null) {
                Assert.assertTrue(scheduleWrapper.monthlyDayScheduleJson == null);
                Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson == null);

                mRemoteWeeklyScheduleRecords.put(id, new RemoteWeeklyScheduleRecord(id, this, scheduleWrapper));
            } else if (scheduleWrapper.monthlyDayScheduleJson != null) {
                Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson == null);

                mRemoteMonthlyDayScheduleRecords.put(id, new RemoteMonthlyDayScheduleRecord(id, this, scheduleWrapper));
            } else {
                Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson != null);

                mRemoteMonthlyWeekScheduleRecords.put(id, new RemoteMonthlyWeekScheduleRecord(id, this, scheduleWrapper));
            }
        }
    }

    @NonNull
    private TaskJson getTaskJson() {
        TaskJson taskJson = mJsonWrapper.taskJson;
        Assert.assertTrue(taskJson != null);

        return taskJson;
    }

    @NonNull
    public String getName() {
        return getTaskJson().getName();
    }

    public long getStartTime() {
        return getTaskJson().getStartTime();
    }

    @Nullable
    public Long getEndTime() {
        return getTaskJson().getEndTime();
    }

    @Nullable
    public String getNote() {
        return getTaskJson().getNote();
    }

    @Nullable
    public Integer getOldestVisibleYear() {
        return getTaskJson().getOldestVisibleYear();
    }

    @Nullable
    public Integer getOldestVisibleMonth() {
        return getTaskJson().getOldestVisibleMonth();
    }

    @Nullable
    public Integer getOldestVisibleDay() {
        return getTaskJson().getOldestVisibleDay();
    }

    @NonNull
    public Map<ScheduleKey, RemoteInstanceRecord> getRemoteInstanceRecords() {
        return mRemoteInstanceRecords;
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        getTaskJson().setEndTime(endTime);
        addValue(getId() + "/" + TASK_JSON + "/endTime", endTime);
    }

    public void setOldestVisibleYear(int oldestVisibleYear) {
        if (getOldestVisibleYear() != null && getOldestVisibleYear().equals(oldestVisibleYear))
            return;

        getTaskJson().setOldestVisibleYear(oldestVisibleYear);
        addValue(getId() + "/" + TASK_JSON + "/oldestVisibleYear", oldestVisibleYear);
    }

    public void setOldestVisibleMonth(int oldestVisibleMonth) {
        if (getOldestVisibleMonth() != null && getOldestVisibleMonth().equals(oldestVisibleMonth))
            return;

        getTaskJson().setOldestVisibleMonth(oldestVisibleMonth);
        addValue(getId() + "/" + TASK_JSON + "/oldestVisibleMonth", oldestVisibleMonth);
    }

    public void setOldestVisibleDay(int oldestVisibleDay) {
        if (getOldestVisibleDay() != null && getOldestVisibleDay().equals(oldestVisibleDay))
            return;

        getTaskJson().setOldestVisibleDay(oldestVisibleDay);
        addValue(getId() + "/" + TASK_JSON + "/oldestVisibleDay", oldestVisibleDay);
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (getName().equals(name))
            return;

        getTaskJson().setName(name);
        addValue(getId() + "/" + TASK_JSON + "/name", name);
    }

    public void setNote(@Nullable String note) {
        if (TextUtils.isEmpty(getNote())) {
            if (TextUtils.isEmpty(note))
                return;
        } else {
            if (getNote().equals(note))
                return;
        }

        getTaskJson().setNote(note);
        addValue(getId() + "/" + TASK_JSON + "/note", note);
    }

    @Override
    void getValues(@NonNull Map<String, Object> values) {
        Assert.assertTrue(!mDeleted);
        Assert.assertTrue(!mCreated);
        Assert.assertTrue(!mUpdated);

        if (mDelete) {
            Log.e("asdf", "RemoteRecord.getValues deleting " + this);

            Assert.assertTrue(!mCreate);
            Assert.assertTrue(mUpdate != null);

            mDeleted = true;
            values.put(getKey(), null);
        } else if (mCreate) {
            Log.e("asdf", "RemoteRecord.getValues creating " + this);

            Assert.assertTrue(mUpdate == null);

            mCreated = true;

            JsonWrapper jsonWrapper = getCreateObject();
            TaskJson taskJson = jsonWrapper.taskJson;

            if (taskJson.getInstances().isEmpty()) { // because of duplicate functionality when converting local task
                taskJson.setInstances(Stream.of(mRemoteInstanceRecords.entrySet())
                        .collect(Collectors.toMap(entry -> RemoteInstanceRecord.scheduleKeyToString(entry.getKey()), entry -> entry.getValue().getCreateObject())));
            }

            Map<String, ScheduleWrapper> scheduleWrappers = new HashMap<>();

            for (RemoteSingleScheduleRecord remoteSingleScheduleRecord : mRemoteSingleScheduleRecords.values())
                scheduleWrappers.put(remoteSingleScheduleRecord.getId(), remoteSingleScheduleRecord.getCreateObject());

            for (RemoteDailyScheduleRecord remoteDailyScheduleRecord : mRemoteDailyScheduleRecords.values())
                scheduleWrappers.put(remoteDailyScheduleRecord.getId(), remoteDailyScheduleRecord.getCreateObject());

            for (RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord : mRemoteWeeklyScheduleRecords.values())
                scheduleWrappers.put(remoteWeeklyScheduleRecord.getId(), remoteWeeklyScheduleRecord.getCreateObject());

            for (RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord : mRemoteMonthlyDayScheduleRecords.values())
                scheduleWrappers.put(remoteMonthlyDayScheduleRecord.getId(), remoteMonthlyDayScheduleRecord.getCreateObject());

            for (RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord : mRemoteMonthlyWeekScheduleRecords.values())
                scheduleWrappers.put(remoteMonthlyWeekScheduleRecord.getId(), remoteMonthlyWeekScheduleRecord.getCreateObject());

            taskJson.setSchedules(scheduleWrappers);

            values.put(getKey(), getCreateObject());
        } else {
            Assert.assertTrue(mUpdate != null);

            if (!mUpdate.isEmpty()) {
                Log.e("asdf", "RemoteRecord.getValues updating " + this);

                mUpdated = true;
                values.putAll(mUpdate);
            }

            for (RemoteInstanceRecord remoteInstanceRecord : mRemoteInstanceRecords.values())
                remoteInstanceRecord.getValues(values);

            for (RemoteSingleScheduleRecord remoteSingleScheduleRecord : mRemoteSingleScheduleRecords.values())
                remoteSingleScheduleRecord.getValues(values);

            for (RemoteDailyScheduleRecord remoteDailyScheduleRecord : mRemoteDailyScheduleRecords.values())
                remoteDailyScheduleRecord.getValues(values);

            for (RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord : mRemoteWeeklyScheduleRecords.values())
                remoteWeeklyScheduleRecord.getValues(values);

            for (RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord : mRemoteMonthlyDayScheduleRecords.values())
                remoteMonthlyDayScheduleRecord.getValues(values);

            for (RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord : mRemoteMonthlyWeekScheduleRecords.values())
                remoteMonthlyWeekScheduleRecord.getValues(values);
        }
    }

    @NonNull
    public RemoteInstanceRecord newRemoteInstanceRecord(@NonNull InstanceJson instanceJson) {
        RemoteInstanceRecord remoteInstanceRecord = new RemoteInstanceRecord(true, this, instanceJson);
        Assert.assertTrue(!mRemoteInstanceRecords.containsKey(remoteInstanceRecord.getScheduleKey()));

        mRemoteInstanceRecords.put(remoteInstanceRecord.getScheduleKey(), remoteInstanceRecord);
        return remoteInstanceRecord;
    }

    @NonNull
    public RemoteSingleScheduleRecord newRemoteSingleScheduleRecord(@NonNull ScheduleWrapper scheduleWrapper) {
        RemoteSingleScheduleRecord remoteSingleScheduleRecord = new RemoteSingleScheduleRecord(this, scheduleWrapper);
        Assert.assertTrue(!mRemoteSingleScheduleRecords.containsKey(remoteSingleScheduleRecord.getId()));

        mRemoteSingleScheduleRecords.put(remoteSingleScheduleRecord.getId(), remoteSingleScheduleRecord);
        return remoteSingleScheduleRecord;
    }

    @NonNull
    public RemoteDailyScheduleRecord newRemoteDailyScheduleRecord(@NonNull ScheduleWrapper scheduleWrapper) {
        RemoteDailyScheduleRecord remoteDailyScheduleRecord = new RemoteDailyScheduleRecord(this, scheduleWrapper);
        Assert.assertTrue(!mRemoteDailyScheduleRecords.containsKey(remoteDailyScheduleRecord.getId()));

        mRemoteDailyScheduleRecords.put(remoteDailyScheduleRecord.getId(), remoteDailyScheduleRecord);
        return remoteDailyScheduleRecord;
    }

    @NonNull
    public RemoteWeeklyScheduleRecord newRemoteWeeklyScheduleRecord(@NonNull ScheduleWrapper scheduleWrapper) {
        RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = new RemoteWeeklyScheduleRecord(this, scheduleWrapper);
        Assert.assertTrue(!mRemoteWeeklyScheduleRecords.containsKey(remoteWeeklyScheduleRecord.getId()));

        mRemoteWeeklyScheduleRecords.put(remoteWeeklyScheduleRecord.getId(), remoteWeeklyScheduleRecord);
        return remoteWeeklyScheduleRecord;
    }

    @NonNull
    public RemoteMonthlyDayScheduleRecord newRemoteMonthlyDayScheduleRecord(@NonNull ScheduleWrapper scheduleWrapper) {
        RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = new RemoteMonthlyDayScheduleRecord(this, scheduleWrapper);
        Assert.assertTrue(!mRemoteMonthlyDayScheduleRecords.containsKey(remoteMonthlyDayScheduleRecord.getId()));

        mRemoteMonthlyDayScheduleRecords.put(remoteMonthlyDayScheduleRecord.getId(), remoteMonthlyDayScheduleRecord);
        return remoteMonthlyDayScheduleRecord;
    }

    @NonNull
    public RemoteMonthlyWeekScheduleRecord newRemoteMonthlyWeekScheduleRecord(@NonNull ScheduleWrapper scheduleWrapper) {
        RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = new RemoteMonthlyWeekScheduleRecord(this, scheduleWrapper);
        Assert.assertTrue(!mRemoteMonthlyWeekScheduleRecords.containsKey(remoteMonthlyWeekScheduleRecord.getId()));

        mRemoteMonthlyWeekScheduleRecords.put(remoteMonthlyWeekScheduleRecord.getId(), remoteMonthlyWeekScheduleRecord);
        return remoteMonthlyWeekScheduleRecord;
    }
}
