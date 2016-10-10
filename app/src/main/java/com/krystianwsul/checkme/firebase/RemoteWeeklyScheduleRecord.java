package com.krystianwsul.checkme.firebase;

import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

public class RemoteWeeklyScheduleRecord extends RemoteScheduleRecord {
    private final int dayOfWeek;

    @Nullable
    private final Integer customTimeId;

    @Nullable
    private final Integer hour;

    @Nullable
    private final Integer minute;

    public RemoteWeeklyScheduleRecord(long startTime, @Nullable Long endTime, int dayOfWeek, @Nullable Integer customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
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
