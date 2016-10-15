package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

public class WeeklyScheduleJson extends ScheduleJson {
    private int dayOfWeek;

    private Integer customTimeId;

    private Integer hour;

    private Integer minute;

    public WeeklyScheduleJson() {

    }

    public WeeklyScheduleJson(long startTime, @Nullable Long endTime, int dayOfWeek, @Nullable Integer customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(startTime, endTime, ScheduleType.WEEKLY.ordinal());

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        this.dayOfWeek = dayOfWeek;

        this.customTimeId = customTimeId;

        this.hour = hour;
        this.minute = minute;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    @Nullable
    public Integer getCustomTimeId() {
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
