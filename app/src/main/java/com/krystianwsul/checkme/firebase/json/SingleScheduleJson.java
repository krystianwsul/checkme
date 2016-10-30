package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

public class SingleScheduleJson extends ScheduleJson {
    private int year;
    private int month;
    private int day;

    private String customTimeId;

    private Integer hour;

    private Integer minute;

    @SuppressWarnings("unused")
    public SingleScheduleJson() {

    }

    public SingleScheduleJson(@NonNull String taskId, long startTime, @Nullable Long endTime, int year, int month, int day, @Nullable String customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(taskId, startTime, endTime, ScheduleType.SINGLE.ordinal());

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || TextUtils.isEmpty(customTimeId));
        Assert.assertTrue((hour != null) || !TextUtils.isEmpty(customTimeId));

        this.year = year;
        this.month = month;
        this.day = day;

        this.customTimeId = customTimeId;

        this.hour = hour;
        this.minute = minute;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    @Nullable
    public String getCustomTimeId() {
        return customTimeId;
    }

    @Nullable
    public Integer getHour() {
        return hour;
    }

    @Nullable
    public Integer getMinute() {
        return minute;
    }
}
