package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

public class WeeklyScheduleDayOfWeekTimeRecord {
    private final int mId;
    private final int mScheduleId;

    private final int mDayOfWeek;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    WeeklyScheduleDayOfWeekTimeRecord(int id, int scheduleId, int dayOfWeek, Integer customTimeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mId = id;
        mScheduleId = scheduleId;

        mDayOfWeek = dayOfWeek;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
    }

    public int getWeeklyScheduleId() {
        return mScheduleId;
    }

    public int getDayOfWeek() {
        return mDayOfWeek;
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
