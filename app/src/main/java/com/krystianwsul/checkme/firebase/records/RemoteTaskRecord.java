package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.TaskJson;

import junit.framework.Assert;

public class RemoteTaskRecord extends RemoteRecord {
    RemoteTaskRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteTaskRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
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

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        getTaskJson().setEndTime(endTime);
        addValue(getId() + "/taskJson/endTime", endTime);
    }

    public void setOldestVisibleYear(int oldestVisibleYear) {
        getTaskJson().setOldestVisibleYear(oldestVisibleYear);
        addValue(getId() + "/taskJson/oldestVisibleYear", oldestVisibleYear);
    }

    public void setOldestVisibleMonth(int oldestVisibleMonth) {
        getTaskJson().setOldestVisibleMonth(oldestVisibleMonth);
        addValue(getId() + "/taskJson/oldestVisibleMonth", oldestVisibleMonth);
    }

    public void setOldestVisibleDay(int oldestVisibleDay) {
        getTaskJson().setOldestVisibleDay(oldestVisibleDay);
        addValue(getId() + "/taskJson/oldestVisibleDay", oldestVisibleDay);
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        getTaskJson().setName(name);
        addValue(getId() + "/taskJson/name", name);
    }

    public void setNote(@Nullable String note) {
        getTaskJson().setNote(note);
        addValue(getId() + "/taskJson/note", note);
    }
}
