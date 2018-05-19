package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;

import junit.framework.Assert;

public class RemoteTaskHierarchyRecord extends RemoteRecord {
    public static final String TASK_HIERARCHIES = "taskHierarchies";

    @NonNull
    private final String mId;

    @NonNull
    private final RemoteProjectRecord mRemoteProjectRecord;

    @NonNull
    private final TaskHierarchyJson mTaskHierarchyJson;

    RemoteTaskHierarchyRecord(@NonNull String id, @NonNull RemoteProjectRecord remoteProjectRecord, @NonNull TaskHierarchyJson taskHierarchyJson) {
        super(false);

        mId = id;
        mRemoteProjectRecord = remoteProjectRecord;
        mTaskHierarchyJson = taskHierarchyJson;
    }

    RemoteTaskHierarchyRecord(@NonNull RemoteProjectRecord remoteProjectRecord, @NonNull TaskHierarchyJson taskHierarchyJson) {
        super(true);

        mId = DatabaseWrapper.INSTANCE.getTaskHierarchyRecordId(remoteProjectRecord.getId());
        mRemoteProjectRecord = remoteProjectRecord;
        mTaskHierarchyJson = taskHierarchyJson;
    }

    @NonNull
    @Override
    public TaskHierarchyJson getCreateObject() {
        return mTaskHierarchyJson;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    @Override
    protected String getKey() {
        return mRemoteProjectRecord.getKey() + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + TASK_HIERARCHIES + "/" + mId;
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

    @Nullable
    public Double getOrdinal() {
        return mTaskHierarchyJson.getOrdinal();
    }

    public void setOrdinal(double ordinal) {
        mTaskHierarchyJson.setOrdinal(ordinal);
        addValue(getKey() + "/ordinal", ordinal);
    }
}
