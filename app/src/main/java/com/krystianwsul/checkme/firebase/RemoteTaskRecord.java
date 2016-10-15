package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class RemoteTaskRecord {
    private String name;

    private long startTime;

    private Long endTime;

    private Integer oldestVisibleYear;

    private Integer oldestVisibleMonth;

    private Integer oldestVisibleDay;

    private String note;

    @Nullable
    private List<RemoteSingleScheduleRecord> singleScheduleRecords;

    @Nullable
    private List<RemoteDailyScheduleRecord> dailyScheduleRecords;

    @Nullable
    private List<RemoteWeeklyScheduleRecord> weeklyScheduleRecords;

    @Nullable
    private List<RemoteMonthlyDayScheduleRecord> monthlyDayScheduleRecords;

    @Nullable
    private List<RemoteMonthlyWeekScheduleRecord> monthlyWeekScheduleRecords;

    public RemoteTaskRecord() {

    }

    public RemoteTaskRecord(@NonNull String name, long startTime, @Nullable Long endTime, @Nullable Integer oldestVisibleYear, @Nullable Integer oldestVisibleMonth, @Nullable Integer oldestVisibleDay, @Nullable String note, @NonNull List<RemoteSingleScheduleRecord> remoteSingleScheduleRecords, @NonNull List<RemoteDailyScheduleRecord> remoteDailyScheduleRecords, @NonNull List<RemoteWeeklyScheduleRecord> remoteWeeklyScheduleRecords, @NonNull List<RemoteMonthlyDayScheduleRecord> remoteMonthlyDayScheduleRecords, @NonNull List<RemoteMonthlyWeekScheduleRecord> remoteMonthlyWeekScheduleRecords) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(endTime == null || startTime <= endTime);
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleMonth == null));
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleDay == null));
        Assert.assertTrue(!remoteSingleScheduleRecords.isEmpty() || !remoteDailyScheduleRecords.isEmpty() || !remoteWeeklyScheduleRecords.isEmpty() || !remoteMonthlyDayScheduleRecords.isEmpty() || !remoteMonthlyWeekScheduleRecords.isEmpty());

        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;

        this.oldestVisibleDay = oldestVisibleDay;
        this.oldestVisibleMonth = oldestVisibleMonth;
        this.oldestVisibleYear = oldestVisibleYear;

        this.note = note;

        singleScheduleRecords = remoteSingleScheduleRecords;
        dailyScheduleRecords = remoteDailyScheduleRecords;
        weeklyScheduleRecords = remoteWeeklyScheduleRecords;
        monthlyDayScheduleRecords = remoteMonthlyDayScheduleRecords;
        monthlyWeekScheduleRecords = remoteMonthlyWeekScheduleRecords;
    }

    public RemoteTaskRecord(@NonNull String name, long startTime, @Nullable Long endTime, @Nullable Integer oldestVisibleYear, @Nullable Integer oldestVisibleMonth, @Nullable Integer oldestVisibleDay, @Nullable String note) {
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

        singleScheduleRecords = null;
        dailyScheduleRecords = null;
        weeklyScheduleRecords = null;
        monthlyDayScheduleRecords = null;
        monthlyWeekScheduleRecords = null;
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
    public List<RemoteSingleScheduleRecord> getSingleScheduleRecords() {
        if (singleScheduleRecords == null)
            return new ArrayList<>();
        else
            return singleScheduleRecords;
    }

    @NonNull
    public List<RemoteDailyScheduleRecord> getDailyScheduleRecords() {
        if (dailyScheduleRecords == null)
            return new ArrayList<>();
        else
            return dailyScheduleRecords;
    }

    @NonNull
    public List<RemoteWeeklyScheduleRecord> getWeeklyScheduleRecords() {
        if (weeklyScheduleRecords == null)
            return new ArrayList<>();
        else
            return weeklyScheduleRecords;
    }

    @NonNull
    public List<RemoteMonthlyDayScheduleRecord> getMonthlyDayScheduleRecords() {
        if (monthlyDayScheduleRecords == null)
            return new ArrayList<>();
        else
            return monthlyDayScheduleRecords;
    }

    @NonNull
    public List<RemoteMonthlyWeekScheduleRecord> getMonthlyWeekScheduleRecords() {
        if (monthlyWeekScheduleRecords == null)
            return new ArrayList<>();
        else
            return monthlyWeekScheduleRecords;
    }
}
