package com.krystianwsul.checkme.firebase;

import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

public class RemoteSingleScheduleRecord extends RemoteScheduleRecord {
    private final int year;
    private final int month;
    private final int day;

    @Nullable
    private final Integer customTimeId;

    @Nullable
    private final Integer hour;

    @Nullable
    private final Integer minute;

    public RemoteSingleScheduleRecord(long startTime, @Nullable Long endTime, int year, int month, int day, @Nullable Integer customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(startTime, endTime, ScheduleType.SINGLE.ordinal());

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

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
