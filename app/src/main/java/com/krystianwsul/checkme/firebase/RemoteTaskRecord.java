package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.List;

public class RemoteTaskRecord {
    @NonNull
    private String name;

    private final long startTime;

    @Nullable
    private Long endTime;

    @Nullable
    private Integer oldestVisibleYear;

    @Nullable
    private Integer oldestVisibleMonth;

    @Nullable
    private Integer oldestVisibleDay;

    @Nullable
    private String note;

    @NonNull
    private final List<RemoteScheduleRecord> scheduleRecords;

    public RemoteTaskRecord(@NonNull String name, long startTime, @Nullable Long endTime, boolean relevant, @Nullable Integer oldestVisibleYear, @Nullable Integer oldestVisibleMonth, @Nullable Integer oldestVisibleDay, @Nullable String note, @NonNull List<RemoteScheduleRecord> scheduleRecords) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(endTime == null || startTime <= endTime);
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleMonth == null));
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleDay == null));
        Assert.assertTrue(!scheduleRecords.isEmpty());

        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;

        this.oldestVisibleDay = oldestVisibleDay;
        this.oldestVisibleMonth = oldestVisibleMonth;
        this.oldestVisibleYear = oldestVisibleYear;

        this.note = note;

        this.scheduleRecords = scheduleRecords;
    }

    @NonNull
    public String getName() {
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
    public List<RemoteScheduleRecord> getScheduleRecords() {
        return scheduleRecords;
    }
}
