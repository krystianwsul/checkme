package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;
public class SingleScheduleDateTimeRecord {
    private final int mScheduleId;

    private final int mYear;
    private final int mMonth;
    private final int mDay;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    SingleScheduleDateTimeRecord(int scheduleId, int year, int month, int day, Integer customTimeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mScheduleId = scheduleId;

        mYear = year;
        mMonth = month;
        mDay = day;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getScheduleId() {
        return mScheduleId;
    }

    public int getYear() {
        return mYear;
    }

    public int getMonth() {
        return mMonth;
    }

    public int getDay() {
        return mDay;
    }

    public Integer getCustomTimeId() {
        return mCustomTimeId;
    }

    public Integer getHour() {
        return mHour;
    }

    public Integer getMinute() {
        return mMinute;
    }
}
