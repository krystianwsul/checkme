package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

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

    private List<RemoteSingleScheduleRecord> singleScheduleRecords;
    private List<RemoteDailyScheduleRecord> dailyScheduleRecords;
    private List<RemoteWeeklyScheduleRecord> weeklyScheduleRecords;
    private List<RemoteMonthlyDayScheduleRecord> monthlyDayScheduleRecords;
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
    public List<RemoteSingleScheduleRecord> getSingleScheduleRecords() {
        return singleScheduleRecords;
    }

    @NonNull
    public List<RemoteDailyScheduleRecord> getDailyScheduleRecords() {
        return dailyScheduleRecords;
    }

    @NonNull
    public List<RemoteWeeklyScheduleRecord> getWeeklyScheduleRecords() {
        return weeklyScheduleRecords;
    }

    @NonNull
    public List<RemoteMonthlyDayScheduleRecord> getMonthlyDayScheduleRecords() {
        return monthlyDayScheduleRecords;
    }

    @NonNull
    public List<RemoteMonthlyWeekScheduleRecord> getMonthlyWeekScheduleRecords() {
        return monthlyWeekScheduleRecords;
    }
}
