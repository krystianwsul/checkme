package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.utils.ScheduleKey;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class RemoteTaskRecord extends RemoteRecord {
    public static final String TASKS = "tasks";

    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final String mId;

    @NonNull
    private final RemoteProjectRecord mRemoteProjectRecord;

    @NonNull
    private final TaskJson mTaskJson;

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

    RemoteTaskRecord(@NonNull DomainFactory domainFactory, @NonNull String id, @NonNull RemoteProjectRecord remoteProjectRecord, @NonNull TaskJson taskJson) {
        super(false);

        mDomainFactory = domainFactory;
        mId = id;
        mRemoteProjectRecord = remoteProjectRecord;
        mTaskJson = taskJson;

        initialize();
    }

    RemoteTaskRecord(@NonNull DomainFactory domainFactory, @NonNull RemoteProjectRecord remoteProjectRecord, @NonNull TaskJson taskJson) {
        super(true);

        mDomainFactory = domainFactory;
        mId = DatabaseWrapper.INSTANCE.getTaskRecordId(remoteProjectRecord.getId());
        mRemoteProjectRecord = remoteProjectRecord;
        mTaskJson = taskJson;

        initialize();
    }

    private void initialize() {
        for (Map.Entry<String, InstanceJson> entry : mTaskJson.getInstances().entrySet()) {
            String key = entry.getKey();
            Assert.assertTrue(!TextUtils.isEmpty(key));

            ScheduleKey scheduleKey = RemoteInstanceRecord.Companion.stringToScheduleKey(mDomainFactory, mRemoteProjectRecord.getId(), key);

            InstanceJson instanceJson = entry.getValue();
            Assert.assertTrue(instanceJson != null);

            RemoteInstanceRecord remoteInstanceRecord = new RemoteInstanceRecord(false, mDomainFactory, this, instanceJson, scheduleKey);

            mRemoteInstanceRecords.put(scheduleKey, remoteInstanceRecord);
        }

        for (Map.Entry<String, ScheduleWrapper> entry : mTaskJson.getSchedules().entrySet()) {
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
    @Override
    protected TaskJson getCreateObject() {
        if (!getCreate()) { // because of duplicate functionality when converting local task
            mTaskJson.setInstances(Stream.of(mRemoteInstanceRecords.entrySet()).collect(Collectors.toMap(entry -> RemoteInstanceRecord.Companion.scheduleKeyToString(mDomainFactory, mRemoteProjectRecord.getId(), entry.getKey()), entry -> entry.getValue().getCreateObject())));
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

        mTaskJson.setSchedules(scheduleWrappers);

        return mTaskJson;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    @Override
    protected String getKey() {
        return mRemoteProjectRecord.getKey() + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + TASKS + "/" + mId;
    }

    @NonNull
    String getProjectId() {
        return mRemoteProjectRecord.getId();
    }

    @NonNull
    public String getName() {
        return mTaskJson.getName();
    }

    public long getStartTime() {
        return mTaskJson.getStartTime();
    }

    @Nullable
    public Long getEndTime() {
        return mTaskJson.getEndTime();
    }

    @Nullable
    public String getNote() {
        return mTaskJson.getNote();
    }

    @Nullable
    public Integer getOldestVisibleYear() {
        return mTaskJson.getOldestVisibleYear();
    }

    @Nullable
    public Integer getOldestVisibleMonth() {
        return mTaskJson.getOldestVisibleMonth();
    }

    @Nullable
    public Integer getOldestVisibleDay() {
        return mTaskJson.getOldestVisibleDay();
    }

    @NonNull
    public Map<ScheduleKey, RemoteInstanceRecord> getRemoteInstanceRecords() {
        return mRemoteInstanceRecords;
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        mTaskJson.setEndTime(endTime);
        addValue(getKey() + "/endTime", endTime);
    }

    public void setOldestVisibleYear(int oldestVisibleYear) {
        if (getOldestVisibleYear() != null && getOldestVisibleYear().equals(oldestVisibleYear))
            return;

        mTaskJson.setOldestVisibleYear(oldestVisibleYear);
        addValue(getKey() + "/oldestVisibleYear", oldestVisibleYear);
    }

    public void setOldestVisibleMonth(int oldestVisibleMonth) {
        if (getOldestVisibleMonth() != null && getOldestVisibleMonth().equals(oldestVisibleMonth))
            return;

        mTaskJson.setOldestVisibleMonth(oldestVisibleMonth);
        addValue(getKey() + "/oldestVisibleMonth", oldestVisibleMonth);
    }

    public void setOldestVisibleDay(int oldestVisibleDay) {
        if (getOldestVisibleDay() != null && getOldestVisibleDay().equals(oldestVisibleDay))
            return;

        mTaskJson.setOldestVisibleDay(oldestVisibleDay);
        addValue(getKey() + "/oldestVisibleDay", oldestVisibleDay);
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (getName().equals(name))
            return;

        mTaskJson.setName(name);
        addValue(getKey() + "/name", name);
    }

    public void setNote(@Nullable String note) {
        if (TextUtils.isEmpty(getNote())) {
            if (TextUtils.isEmpty(note))
                return;
        } else {
            if (getNote().equals(note))
                return;
        }

        mTaskJson.setNote(note);
        addValue(getKey() + "/note", note);
    }

    @Override
    public void getValues(@NonNull Map<String, Object> values) {
        Assert.assertTrue(!getDeleted());
        Assert.assertTrue(!getCreated());
        Assert.assertTrue(!getUpdated());

        if (getDelete()) {
            Log.e("asdf", "RemoteTaskRecord.getValues deleting " + this);

            Assert.assertTrue(!getCreate());
            Assert.assertTrue(getUpdate() != null);

            setDeleted(true);
            values.put(getKey(), null);
        } else if (getCreate()) {
            Log.e("asdf", "RemoteTaskRecord.getValues creating " + this);

            Assert.assertTrue(getUpdate() == null);

            setCreated(true);

            values.put(getKey(), getCreateObject());
        } else {
            Assert.assertTrue(getUpdate() != null);

            if (!getUpdate().isEmpty()) {
                Log.e("asdf", "RemoteTaskRecord.getValues updating " + this);

                setUpdated(true);
                values.putAll(getUpdate());
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
    public RemoteInstanceRecord newRemoteInstanceRecord(@NonNull DomainFactory domainFactory, @NonNull InstanceJson instanceJson, @NonNull ScheduleKey scheduleKey) {
        RemoteInstanceRecord remoteInstanceRecord = new RemoteInstanceRecord(true, domainFactory, this, instanceJson, scheduleKey);
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
