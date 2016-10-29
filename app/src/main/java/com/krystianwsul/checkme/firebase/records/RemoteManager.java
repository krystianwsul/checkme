package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class RemoteManager {
    private boolean mSaved = false;

    @NonNull
    public final Map<String, RemoteTaskRecord> mRemoteTaskRecords = new HashMap<>();

    @NonNull
    public final Map<String, RemoteTaskHierarchyRecord> mRemoteTaskHierarchyRecords = new HashMap<>();

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

    @NonNull
    public final Map<String, RemoteInstanceRecord> mRemoteInstanceRecords = new HashMap<>();

    public RemoteManager(@NonNull Iterable<DataSnapshot> children) {
        for (DataSnapshot child : children) {
            Assert.assertTrue(child != null);

            String key = child.getKey();
            Assert.assertTrue(!TextUtils.isEmpty(key));

            JsonWrapper jsonWrapper = child.getValue(JsonWrapper.class);
            Assert.assertTrue(jsonWrapper != null);

            if (jsonWrapper.taskJson != null) {
                Assert.assertTrue(jsonWrapper.taskHierarchyJson == null);
                Assert.assertTrue(jsonWrapper.singleScheduleJson == null);
                Assert.assertTrue(jsonWrapper.dailyScheduleJson == null);
                Assert.assertTrue(jsonWrapper.weeklyScheduleJson == null);
                Assert.assertTrue(jsonWrapper.monthlyDayScheduleJson == null);
                Assert.assertTrue(jsonWrapper.monthlyWeekScheduleJson == null);
                Assert.assertTrue(jsonWrapper.instanceJson == null);

                mRemoteTaskRecords.put(key, new RemoteTaskRecord(key, jsonWrapper));
            } else if (jsonWrapper.taskHierarchyJson != null) {
                Assert.assertTrue(jsonWrapper.singleScheduleJson == null);
                Assert.assertTrue(jsonWrapper.dailyScheduleJson == null);
                Assert.assertTrue(jsonWrapper.weeklyScheduleJson == null);
                Assert.assertTrue(jsonWrapper.monthlyDayScheduleJson == null);
                Assert.assertTrue(jsonWrapper.monthlyWeekScheduleJson == null);
                Assert.assertTrue(jsonWrapper.instanceJson == null);

                mRemoteTaskHierarchyRecords.put(key, new RemoteTaskHierarchyRecord(key, jsonWrapper));
            } else if (jsonWrapper.singleScheduleJson != null) {
                Assert.assertTrue(jsonWrapper.dailyScheduleJson == null);
                Assert.assertTrue(jsonWrapper.weeklyScheduleJson == null);
                Assert.assertTrue(jsonWrapper.monthlyDayScheduleJson == null);
                Assert.assertTrue(jsonWrapper.monthlyWeekScheduleJson == null);
                Assert.assertTrue(jsonWrapper.instanceJson == null);

                mRemoteSingleScheduleRecords.put(key, new RemoteSingleScheduleRecord(key, jsonWrapper));
            } else if (jsonWrapper.dailyScheduleJson != null) {
                Assert.assertTrue(jsonWrapper.weeklyScheduleJson == null);
                Assert.assertTrue(jsonWrapper.monthlyDayScheduleJson == null);
                Assert.assertTrue(jsonWrapper.monthlyWeekScheduleJson == null);
                Assert.assertTrue(jsonWrapper.instanceJson == null);

                mRemoteDailyScheduleRecords.put(key, new RemoteDailyScheduleRecord(key, jsonWrapper));
            } else if (jsonWrapper.weeklyScheduleJson != null) {
                Assert.assertTrue(jsonWrapper.monthlyDayScheduleJson == null);
                Assert.assertTrue(jsonWrapper.monthlyWeekScheduleJson == null);
                Assert.assertTrue(jsonWrapper.instanceJson == null);

                mRemoteWeeklyScheduleRecords.put(key, new RemoteWeeklyScheduleRecord(key, jsonWrapper));
            } else if (jsonWrapper.monthlyDayScheduleJson != null) {
                Assert.assertTrue(jsonWrapper.monthlyWeekScheduleJson == null);
                Assert.assertTrue(jsonWrapper.instanceJson == null);

                mRemoteMonthlyDayScheduleRecords.put(key, new RemoteMonthlyDayScheduleRecord(key, jsonWrapper));
            } else if (jsonWrapper.monthlyWeekScheduleJson != null) {
                Assert.assertTrue(jsonWrapper.instanceJson == null);

                mRemoteMonthlyWeekScheduleRecords.put(key, new RemoteMonthlyWeekScheduleRecord(key, jsonWrapper));
            } else {
                Assert.assertTrue(jsonWrapper.instanceJson != null);

                mRemoteInstanceRecords.put(key, new RemoteInstanceRecord(key, jsonWrapper));
            }
        }
    }

    public void save() {
        Map<String, Object> values = new HashMap<>();

        Stream.of(mRemoteTaskRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteTaskHierarchyRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteSingleScheduleRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteDailyScheduleRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteWeeklyScheduleRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteMonthlyDayScheduleRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteMonthlyWeekScheduleRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteInstanceRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Log.e("asdf", "RemoteManager.save values: " + values);

        if (!values.isEmpty()) {
            mSaved = true;
            DatabaseWrapper.updateRecords(values);
        }
    }

    public boolean isSaved() {
        return mSaved;
    }

    @NonNull
    public RemoteTaskRecord newRemoteTaskRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteTaskRecord remoteTaskRecord = new RemoteTaskRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteTaskRecords.containsKey(remoteTaskRecord.getId()));

        mRemoteTaskRecords.put(remoteTaskRecord.getId(), remoteTaskRecord);
        return remoteTaskRecord;
    }

    @NonNull
    public RemoteTaskHierarchyRecord newRemoteTaskHierarchyRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = new RemoteTaskHierarchyRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteTaskHierarchyRecords.containsKey(remoteTaskHierarchyRecord.getId()));

        mRemoteTaskHierarchyRecords.put(remoteTaskHierarchyRecord.getId(), remoteTaskHierarchyRecord);
        return remoteTaskHierarchyRecord;
    }

    @NonNull
    public RemoteSingleScheduleRecord newRemoteSingleScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteSingleScheduleRecord remoteSingleScheduleRecord = new RemoteSingleScheduleRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteSingleScheduleRecords.containsKey(remoteSingleScheduleRecord.getId()));

        mRemoteSingleScheduleRecords.put(remoteSingleScheduleRecord.getId(), remoteSingleScheduleRecord);
        return remoteSingleScheduleRecord;
    }

    @NonNull
    public RemoteDailyScheduleRecord newRemoteDailyScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteDailyScheduleRecord remoteDailyScheduleRecord = new RemoteDailyScheduleRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteDailyScheduleRecords.containsKey(remoteDailyScheduleRecord.getId()));

        mRemoteDailyScheduleRecords.put(remoteDailyScheduleRecord.getId(), remoteDailyScheduleRecord);
        return remoteDailyScheduleRecord;
    }

    @NonNull
    public RemoteWeeklyScheduleRecord newRemoteWeeklyScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord = new RemoteWeeklyScheduleRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteWeeklyScheduleRecords.containsKey(remoteWeeklyScheduleRecord.getId()));

        mRemoteWeeklyScheduleRecords.put(remoteWeeklyScheduleRecord.getId(), remoteWeeklyScheduleRecord);
        return remoteWeeklyScheduleRecord;
    }

    @NonNull
    public RemoteMonthlyDayScheduleRecord newRemoteMonthlyDayScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteMonthlyDayScheduleRecord remoteMonthlyDayScheduleRecord = new RemoteMonthlyDayScheduleRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteMonthlyDayScheduleRecords.containsKey(remoteMonthlyDayScheduleRecord.getId()));

        mRemoteMonthlyDayScheduleRecords.put(remoteMonthlyDayScheduleRecord.getId(), remoteMonthlyDayScheduleRecord);
        return remoteMonthlyDayScheduleRecord;
    }

    @NonNull
    public RemoteMonthlyWeekScheduleRecord newRemoteMonthlyWeekScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord = new RemoteMonthlyWeekScheduleRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteMonthlyWeekScheduleRecords.containsKey(remoteMonthlyWeekScheduleRecord.getId()));

        mRemoteMonthlyWeekScheduleRecords.put(remoteMonthlyWeekScheduleRecord.getId(), remoteMonthlyWeekScheduleRecord);
        return remoteMonthlyWeekScheduleRecord;
    }

    @NonNull
    public RemoteInstanceRecord newRemoteInstanceRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteInstanceRecord remoteInstanceRecord = new RemoteInstanceRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteInstanceRecords.containsKey(remoteInstanceRecord.getId()));

        mRemoteInstanceRecords.put(remoteInstanceRecord.getId(), remoteInstanceRecord);
        return remoteInstanceRecord;
    }
}
