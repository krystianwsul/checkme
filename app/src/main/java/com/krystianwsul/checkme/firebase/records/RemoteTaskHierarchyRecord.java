package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;

import junit.framework.Assert;

import java.util.Set;

public class RemoteTaskHierarchyRecord extends RemoteRecord {
    public static final String TASKHIERARCHIES = "taskHierarchies";

    @NonNull
    private final String mId;

    @NonNull
    private final RemoteProjectRecord mRemoteProjectRecord;

    @NonNull
    private final TaskHierarchyJson mTaskHierarchyJson;

    RemoteTaskHierarchyRecord(@NonNull String id, @NonNull RemoteProjectRecord remoteProjectRecord, @NonNull TaskHierarchyJson taskHierarchyJson) {
        super(true);

        mId = id;
        mRemoteProjectRecord = remoteProjectRecord;
        mTaskHierarchyJson = taskHierarchyJson;
    }

    RemoteTaskHierarchyRecord(@NonNull RemoteProjectRecord remoteProjectRecord, @NonNull TaskHierarchyJson taskHierarchyJson) {
        super(false);

        mId = DatabaseWrapper.getTaskHierarchyRecordId(remoteProjectRecord.getId());
        mRemoteProjectRecord = remoteProjectRecord;
        mTaskHierarchyJson = taskHierarchyJson;
    }

    @NonNull
    @Override
    protected TaskHierarchyJson getCreateObject() {
        return mTaskHierarchyJson;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    @Override
    protected String getKey() {
        return mRemoteProjectRecord.getKey() + "/" + TASKHIERARCHIES + "/" + mId;
    }

    public long getStartTime() {
        return mTaskHierarchyJson.getStartTime();
    }

    @Nullable
    public Long getEndTime() {
        return mTaskHierarchyJson.getEndTime();
    }

    @NonNull
    public String getParentTaskId() {
        return mTaskHierarchyJson.getParentTaskId();
    }

    @NonNull
    public String getChildTaskId() {
        return mTaskHierarchyJson.getChildTaskId();
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        mTaskHierarchyJson.setEndTime(endTime);
        addValue(getKey() + "/endTime", endTime);
    }

    @NonNull
    public Set<String> getRecordOf() { // todo remove once projects are in place
        return mRemoteProjectRecord.getRecordOf();
    }

    public void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) { // todo remove once projects are in place
        mRemoteProjectRecord.updateRecordOf(addedFriends, removedFriends);
    }
}
