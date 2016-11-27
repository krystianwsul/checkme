package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.ProjectJson;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class RemoteProjectRecord extends RootRemoteRecord {
    public static final String PROJECT_JSON = "projectJson";

    @NonNull
    private final Map<String, RemoteTaskRecord> mRemoteTaskRecords = new HashMap<>();

    @NonNull
    private final Map<String, RemoteTaskHierarchyRecord> mRemoteTaskHierarchyRecords = new HashMap<>();

    RemoteProjectRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);

        initialize();
    }

    RemoteProjectRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);

        initialize();
    }

    private void initialize() {
        for (Map.Entry<String, TaskJson> entry : getProjectJson().getTasks().entrySet()) {
            String id = entry.getKey();
            Assert.assertTrue(!TextUtils.isEmpty(id));

            TaskJson taskJson = entry.getValue();
            Assert.assertTrue(taskJson != null);

            mRemoteTaskRecords.put(id, new RemoteTaskRecord(id, this, taskJson));
        }

        for (Map.Entry<String, TaskHierarchyJson> entry : getProjectJson().getTaskHierarchies().entrySet()) {
            TaskHierarchyJson taskHierarchyJson = entry.getValue();
            Assert.assertTrue(taskHierarchyJson != null);

            String id = entry.getKey();
            Assert.assertTrue(!TextUtils.isEmpty(id));

            mRemoteTaskHierarchyRecords.put(id, new RemoteTaskHierarchyRecord(id, this, taskHierarchyJson));
        }
    }

    @NonNull
    @Override
    protected JsonWrapper getCreateObject() {
        JsonWrapper jsonWrapper = super.getCreateObject();

        ProjectJson projectJson = jsonWrapper.projectJson;
        Assert.assertTrue(projectJson != null);

        projectJson.setTasks(Stream.of(mRemoteTaskRecords.values())
                .collect(Collectors.toMap(RemoteTaskRecord::getId, RemoteTaskRecord::getCreateObject)));

        projectJson.setTaskHierarchies(Stream.of(mRemoteTaskHierarchyRecords.values())
                .collect(Collectors.toMap(RemoteTaskHierarchyRecord::getId, RemoteTaskHierarchyRecord::getCreateObject)));

        return jsonWrapper;
    }

    @NonNull
    private ProjectJson getProjectJson() {
        ProjectJson projectJson = mJsonWrapper.projectJson;
        Assert.assertTrue(projectJson != null);

        return projectJson;
    }

    @NonNull
    public String getName() {
        return getProjectJson().getName();
    }

    public long getStartTime() {
        return getProjectJson().getStartTime();
    }

    @Nullable
    public Long getEndTime() {
        return getProjectJson().getEndTime();
    }

    @NonNull
    Map<String, RemoteTaskRecord> getRemoteTaskRecords() {
        return mRemoteTaskRecords;
    }

    @NonNull
    Map<String, RemoteTaskHierarchyRecord> getRemoteTaskHierarchyRecords() {
        return mRemoteTaskHierarchyRecords;
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        getProjectJson().setEndTime(endTime);
        addValue(getId() + "/" + PROJECT_JSON + "/endTime", endTime);
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (getName().equals(name))
            return;

        getProjectJson().setName(name);
        addValue(getId() + "/" + PROJECT_JSON + "/name", name);
    }

    @Override
    void getValues(@NonNull Map<String, Object> values) {
        Assert.assertTrue(!mDeleted);
        Assert.assertTrue(!mCreated);
        Assert.assertTrue(!mUpdated);

        if (mDelete) {
            Log.e("asdf", "RemoteProjectRecord.getValues deleting " + this);

            Assert.assertTrue(!mCreate);
            Assert.assertTrue(mUpdate != null);

            mDeleted = true;
            values.put(getKey(), null);
        } else if (mCreate) {
            Log.e("asdf", "RemoteProjectRecord.getValues creating " + this);

            Assert.assertTrue(mUpdate == null);

            mCreated = true;

            values.put(getKey(), getCreateObject());
        } else {
            Assert.assertTrue(mUpdate != null);

            if (!mUpdate.isEmpty()) {
                Log.e("asdf", "RemoteProjectRecord.getValues updating " + this);

                mUpdated = true;
                values.putAll(mUpdate);
            }

            for (RemoteTaskRecord remoteTaskRecord : mRemoteTaskRecords.values())
                remoteTaskRecord.getValues(values);

            for (RemoteTaskHierarchyRecord remoteTaskHierarchyRecord : mRemoteTaskHierarchyRecords.values())
                remoteTaskHierarchyRecord.getValues(values);
        }
    }

    @NonNull
    RemoteTaskRecord newRemoteTaskRecord(@NonNull TaskJson taskJson) {
        RemoteTaskRecord remoteTaskRecord = new RemoteTaskRecord(this, taskJson);
        Assert.assertTrue(!mRemoteTaskRecords.containsKey(remoteTaskRecord.getId()));

        mRemoteTaskRecords.put(remoteTaskRecord.getId(), remoteTaskRecord);
        return remoteTaskRecord;
    }

    @NonNull
    RemoteTaskHierarchyRecord newRemoteTaskHierarchyRecord(@NonNull TaskHierarchyJson taskHierarchyJson) {
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = new RemoteTaskHierarchyRecord(this, taskHierarchyJson);
        Assert.assertTrue(!mRemoteTaskHierarchyRecords.containsKey(remoteTaskHierarchyRecord.getId()));

        mRemoteTaskHierarchyRecords.put(remoteTaskHierarchyRecord.getId(), remoteTaskHierarchyRecord);
        return remoteTaskHierarchyRecord;
    }
}
