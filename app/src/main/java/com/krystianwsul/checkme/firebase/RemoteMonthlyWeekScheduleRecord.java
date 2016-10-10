package com.krystianwsul.checkme.firebase;

import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

public class RemoteMonthlyWeekScheduleRecord extends RemoteScheduleRecord {
    private final int dayOfMonth;
    private final int dayOfWeek;
    private final boolean beginningOfMonth;

    @Nullable
    private final Integer customTimeId;

    @Nullable
    private final Integer hour;

    @Nullable
    private final Integer minute;

    public RemoteMonthlyWeekScheduleRecord(long startTime, @Nullable Long endTime, int dayOfMonth, int dayOfWeek, boolean beginningOfMonth, @Nullable Integer customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(startTime, endTime, ScheduleType.MONTHLY_WEEK.ordinal());

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek = dayOfWeek;
        this.beginningOfMonth = beginningOfMonth;

        this.customTimeId = customTimeId;

        this.hour = hour;
        this.minute = minute;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public boolean getBeginningOfMonth() {
        return beginningOfMonth;
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
