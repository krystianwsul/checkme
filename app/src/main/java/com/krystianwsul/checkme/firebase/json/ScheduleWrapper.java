package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;

import com.google.firebase.database.IgnoreExtraProperties;

@SuppressWarnings({"unused", "WeakerAccess"})
@IgnoreExtraProperties
public class ScheduleWrapper {
    public SingleScheduleJson singleScheduleJson;
    public DailyScheduleJson dailyScheduleJson;
    public WeeklyScheduleJson weeklyScheduleJson;
    public MonthlyDayScheduleJson monthlyDayScheduleJson;
    public MonthlyWeekScheduleJson monthlyWeekScheduleJson;

    public ScheduleWrapper() {

    }

    public ScheduleWrapper(@NonNull SingleScheduleJson singleScheduleJson) {
        this.singleScheduleJson = singleScheduleJson;
    }

    public ScheduleWrapper(@NonNull DailyScheduleJson dailyScheduleJson) {
        this.dailyScheduleJson = dailyScheduleJson;
    }

    public ScheduleWrapper(@NonNull WeeklyScheduleJson weeklyScheduleJson) {
        this.weeklyScheduleJson = weeklyScheduleJson;
    }

    public ScheduleWrapper(@NonNull MonthlyDayScheduleJson monthlyDayScheduleJson) {
        this.monthlyDayScheduleJson = monthlyDayScheduleJson;
    }

    public ScheduleWrapper(@NonNull MonthlyWeekScheduleJson monthlyWeekScheduleJson) {
        this.monthlyWeekScheduleJson = monthlyWeekScheduleJson;
    }
}
