package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class ProjectJson {
    @Nullable
    private String name;

    private long startTime;

    @Nullable
    private Long endTime;

    @Nullable
    private Map<String, TaskJson> tasks;

    @Nullable
    private Map<String, TaskHierarchyJson> taskHierarchies;

    @Nullable
    private Map<String, CustomTimeJson> customTimes;

    public ProjectJson() {

    }

    public ProjectJson(@NonNull String name, long startTime, @Nullable Long endTime, @NonNull Map<String, TaskJson> tasks, @NonNull Map<String, TaskHierarchyJson> taskHierarchies, @NonNull Map<String, CustomTimeJson> customTimes) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(endTime == null || startTime <= endTime);

        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;

        this.tasks = tasks;
        this.taskHierarchies = taskHierarchies;
        this.customTimes = customTimes;
    }

    @NonNull
    public String getName() {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        return name;
    }

    public long getStartTime() {
        return startTime;
    }

    @Nullable
    public Long getEndTime() {
        return endTime;
    }

    @NonNull
    public Map<String, TaskJson> getTasks() {
        if (tasks == null)
            return new HashMap<>();
        else
            return tasks;
    }

    @NonNull
    public Map<String, TaskHierarchyJson> getTaskHierarchies() {
        if (taskHierarchies == null)
            return new HashMap<>();
        else
            return taskHierarchies;
    }

    @NonNull
    public Map<String, CustomTimeJson> getCustomTimes() {
        if (customTimes == null)
            return new HashMap<>();
        else
            return customTimes;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        this.name = name;
    }

    public void setTasks(@NonNull Map<String, TaskJson> tasks) {
        this.tasks = tasks;
    }

    public void setTaskHierarchies(@NonNull Map<String, TaskHierarchyJson> taskHierarchies) {
        this.taskHierarchies = taskHierarchies;
    }

    public void setCustomTimes(@NonNull Map<String, CustomTimeJson> customTimes) {
        this.customTimes = customTimes;
    }
}
