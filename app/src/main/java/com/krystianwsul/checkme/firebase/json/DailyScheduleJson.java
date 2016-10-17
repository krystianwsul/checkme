package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

public class DailyScheduleJson extends ScheduleJson {
    private Integer customTimeId;

    private Integer hour;

    private Integer minute;

    public DailyScheduleJson() {

    }

    public DailyScheduleJson(@NonNull String taskId, long startTime, @Nullable Long endTime, @Nullable Integer customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(taskId, startTime, endTime, ScheduleType.DAILY.ordinal());

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        this.customTimeId = customTimeId;

        this.hour = hour;
        this.minute = minute;
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
