package com.krystianwsul.checkme.firebase;

import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

public class RemoteMonthlyWeekScheduleRecord extends RemoteScheduleRecord {
    private int dayOfMonth;
    private int dayOfWeek;
    private boolean beginningOfMonth;

    private Integer customTimeId;

    private Integer hour;

    private Integer minute;

    public RemoteMonthlyWeekScheduleRecord() {

    }

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
