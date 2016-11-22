package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

public class DailyScheduleJson extends ScheduleJson {
    private String customTimeId;

    private Integer hour;

    private Integer minute;

    @SuppressWarnings("unused")
    public DailyScheduleJson() {

    }

    public DailyScheduleJson(long startTime, @Nullable Long endTime, @Nullable String customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(startTime, endTime);

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || TextUtils.isEmpty(customTimeId));
        Assert.assertTrue((hour != null) || !TextUtils.isEmpty(customTimeId));

        this.customTimeId = customTimeId;

        this.hour = hour;
        this.minute = minute;
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
