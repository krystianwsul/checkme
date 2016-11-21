package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.utils.ScheduleKey;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class RemoteTaskRecord extends RootRemoteRecord {
    @NonNull
    private final Map<ScheduleKey, RemoteInstanceRecord> mRemoteInstanceRecords = new HashMap<>();

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
        addValue(getId() + "/taskJson/endTime", endTime);
    }

    public void setOldestVisibleYear(int oldestVisibleYear) {
        if (getOldestVisibleYear() != null && getOldestVisibleYear().equals(oldestVisibleYear))
            return;

        getTaskJson().setOldestVisibleYear(oldestVisibleYear);
        addValue(getId() + "/taskJson/oldestVisibleYear", oldestVisibleYear);
    }

    public void setOldestVisibleMonth(int oldestVisibleMonth) {
        if (getOldestVisibleMonth() != null && getOldestVisibleMonth().equals(oldestVisibleMonth))
            return;

        getTaskJson().setOldestVisibleMonth(oldestVisibleMonth);
        addValue(getId() + "/taskJson/oldestVisibleMonth", oldestVisibleMonth);
    }

    public void setOldestVisibleDay(int oldestVisibleDay) {
        if (getOldestVisibleDay() != null && getOldestVisibleDay().equals(oldestVisibleDay))
            return;

        getTaskJson().setOldestVisibleDay(oldestVisibleDay);
        addValue(getId() + "/taskJson/oldestVisibleDay", oldestVisibleDay);
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (getName().equals(name))
            return;

        getTaskJson().setName(name);
        addValue(getId() + "/taskJson/name", name);
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
        addValue(getId() + "/taskJson/note", note);
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
            values.put(getKey(), getCreateObject());

            for (RemoteInstanceRecord remoteInstanceRecord : mRemoteInstanceRecords.values())
                remoteInstanceRecord.getValues(values);
        } else {
            Assert.assertTrue(mUpdate != null);

            if (!mUpdate.isEmpty()) {
                Log.e("asdf", "RemoteRecord.getValues updating " + this);

                mUpdated = true;
                values.putAll(mUpdate);
            }

            for (RemoteInstanceRecord remoteInstanceRecord : mRemoteInstanceRecords.values())
                remoteInstanceRecord.getValues(values);
        }
    }

    @NonNull
    public RemoteInstanceRecord newRemoteInstanceRecord(@NonNull InstanceJson instanceJson) {
        RemoteInstanceRecord remoteInstanceRecord = new RemoteInstanceRecord(true, this, instanceJson);
        Assert.assertTrue(!mRemoteInstanceRecords.containsKey(remoteInstanceRecord.getScheduleKey()));

        mRemoteInstanceRecords.put(remoteInstanceRecord.getScheduleKey(), remoteInstanceRecord);
        return remoteInstanceRecord;
    }
}
