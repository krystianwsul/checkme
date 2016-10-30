package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

public class WeeklyScheduleJson extends ScheduleJson {
    private int dayOfWeek;

    private String customTimeId;

    private Integer hour;

    private Integer minute;

    @SuppressWarnings("unused")
    public WeeklyScheduleJson() {

    }

    public WeeklyScheduleJson(@NonNull String taskId, long startTime, @Nullable Long endTime, int dayOfWeek, @Nullable String customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(taskId, startTime, endTime, ScheduleType.WEEKLY.ordinal());

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) != TextUtils.isEmpty(customTimeId));

        this.dayOfWeek = dayOfWeek;

        this.customTimeId = customTimeId;

        this.hour = hour;
        this.minute = minute;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
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
