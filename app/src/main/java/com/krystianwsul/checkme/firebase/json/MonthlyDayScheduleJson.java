package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

public class MonthlyDayScheduleJson extends ScheduleJson {
    private int dayOfMonth;
    private boolean beginningOfMonth;

    private String customTimeId;

    private Integer hour;

    private Integer minute;

    @SuppressWarnings("unused")
    public MonthlyDayScheduleJson() {

    }

    public MonthlyDayScheduleJson(long startTime, @Nullable Long endTime, int dayOfMonth, boolean beginningOfMonth, @Nullable String customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(startTime, endTime);

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || TextUtils.isEmpty(customTimeId));
        Assert.assertTrue((hour != null) || !TextUtils.isEmpty(customTimeId));

        this.dayOfMonth = dayOfMonth;
        this.beginningOfMonth = beginningOfMonth;

        this.customTimeId = customTimeId;

        this.hour = hour;
        this.minute = minute;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public boolean getBeginningOfMonth() {
        return beginningOfMonth;
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
