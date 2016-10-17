package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

@SuppressWarnings("unused")
public class TaskJson {
    private String name;

    private long startTime;

    private Long endTime;

    private Integer oldestVisibleYear;

    private Integer oldestVisibleMonth;

    private Integer oldestVisibleDay;

    private String note;

    public TaskJson() {

    }

    public TaskJson(@NonNull String name, long startTime, @Nullable Long endTime, @Nullable Integer oldestVisibleYear, @Nullable Integer oldestVisibleMonth, @Nullable Integer oldestVisibleDay, @Nullable String note) {
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
}
