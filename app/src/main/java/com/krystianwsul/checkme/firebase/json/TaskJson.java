package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

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
    private List<SingleScheduleJson> singleScheduleRecords;

    @Nullable
    private List<DailyScheduleJson> dailyScheduleRecords;

    @Nullable
    private List<WeeklyScheduleJson> weeklyScheduleRecords;

    @Nullable
    private List<MonthlyDayScheduleJson> monthlyDayScheduleRecords;

    @Nullable
    private List<MonthlyWeekScheduleJson> monthlyWeekScheduleRecords;

    public TaskJson() {

    }

    public TaskJson(@NonNull String name, long startTime, @Nullable Long endTime, @Nullable Integer oldestVisibleYear, @Nullable Integer oldestVisibleMonth, @Nullable Integer oldestVisibleDay, @Nullable String note, @NonNull List<SingleScheduleJson> remoteSingleScheduleRecords, @NonNull List<DailyScheduleJson> dailyScheduleJsons, @NonNull List<WeeklyScheduleJson> remoteWeeklyScheduleRecords, @NonNull List<MonthlyDayScheduleJson> monthlyDayScheduleJsons, @NonNull List<MonthlyWeekScheduleJson> monthlyWeekScheduleJsons) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(endTime == null || startTime <= endTime);
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleMonth == null));
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleDay == null));
        Assert.assertTrue(!remoteSingleScheduleRecords.isEmpty() || !dailyScheduleJsons.isEmpty() || !remoteWeeklyScheduleRecords.isEmpty() || !monthlyDayScheduleJsons.isEmpty() || !monthlyWeekScheduleJsons.isEmpty());

        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;

        this.oldestVisibleDay = oldestVisibleDay;
        this.oldestVisibleMonth = oldestVisibleMonth;
        this.oldestVisibleYear = oldestVisibleYear;

        this.note = note;

        singleScheduleRecords = remoteSingleScheduleRecords;
        dailyScheduleRecords = dailyScheduleJsons;
        weeklyScheduleRecords = remoteWeeklyScheduleRecords;
        monthlyDayScheduleRecords = monthlyDayScheduleJsons;
        monthlyWeekScheduleRecords = monthlyWeekScheduleJsons;
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
    public List<SingleScheduleJson> getSingleScheduleRecords() {
        if (singleScheduleRecords == null)
            return new ArrayList<>();
        else
            return singleScheduleRecords;
    }

    @NonNull
    public List<DailyScheduleJson> getDailyScheduleRecords() {
        if (dailyScheduleRecords == null)
            return new ArrayList<>();
        else
            return dailyScheduleRecords;
    }

    @NonNull
    public List<WeeklyScheduleJson> getWeeklyScheduleRecords() {
        if (weeklyScheduleRecords == null)
            return new ArrayList<>();
        else
            return weeklyScheduleRecords;
    }

    @NonNull
    public List<MonthlyDayScheduleJson> getMonthlyDayScheduleRecords() {
        if (monthlyDayScheduleRecords == null)
            return new ArrayList<>();
        else
            return monthlyDayScheduleRecords;
    }

    @NonNull
    public List<MonthlyWeekScheduleJson> getMonthlyWeekScheduleRecords() {
        if (monthlyWeekScheduleRecords == null)
            return new ArrayList<>();
        else
            return monthlyWeekScheduleRecords;
    }
}
