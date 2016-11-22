package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class TaskJson {
    private String name;

    private long startTime;

    private Long endTime;

    private Integer oldestVisibleYear;

    private Integer oldestVisibleMonth;

    private Integer oldestVisibleDay;

    private String note;

    @Nullable
    private Map<String, InstanceJson> instances;

    @Nullable
    private Map<String, ScheduleWrapper> schedules;

    public TaskJson() {

    }

    public TaskJson(@NonNull String name, long startTime, @Nullable Long endTime, @Nullable Integer oldestVisibleYear, @Nullable Integer oldestVisibleMonth, @Nullable Integer oldestVisibleDay, @Nullable String note, @NonNull Map<String, InstanceJson> instances) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(endTime == null || startTime <= endTime);
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleMonth == null));
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleDay == null));

        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;

        this.oldestVisibleDay = oldestVisibleDay;
        this.oldestVisibleMonth = oldestVisibleMonth;
        this.oldestVisibleYear = oldestVisibleYear;

        this.note = note;

        this.instances = instances;
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

    @Nullable
    public Integer getOldestVisibleYear() {
        return oldestVisibleYear;
    }

    @Nullable
    public Integer getOldestVisibleMonth() {
        return oldestVisibleMonth;
    }

    @Nullable
    public Integer getOldestVisibleDay() {
        return oldestVisibleDay;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    @NonNull
    public Map<String, InstanceJson> getInstances() {
        if (instances == null)
            return new HashMap<>();
        else
            return instances;
    }

    @NonNull
    public Map<String, ScheduleWrapper> getSchedules() {
        if (schedules == null)
            return new HashMap<>();
        else
            return schedules;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setOldestVisibleYear(int oldestVisibleYear) {
        this.oldestVisibleYear = oldestVisibleYear;
    }

    public void setOldestVisibleMonth(int oldestVisibleMonth) {
        this.oldestVisibleMonth = oldestVisibleMonth;
    }

    public void setOldestVisibleDay(int oldestVisibleDay) {
        this.oldestVisibleDay = oldestVisibleDay;
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        this.name = name;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
    }

    public void setInstances(@NonNull Map<String, InstanceJson> instances) {
        this.instances = instances;
    }

    public void setSchedules(@NonNull Map<String, ScheduleWrapper> schedules) {
        this.schedules = schedules;
    }
}
