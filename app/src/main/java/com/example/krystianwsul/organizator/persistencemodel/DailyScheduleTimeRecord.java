package com.example.krystianwsul.organizator.persistencemodel;

import junit.framework.Assert;

public class DailyScheduleTimeRecord {
    private final int mId;
    private final int mScheduleId;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    DailyScheduleTimeRecord(int id, int scheduleId, Integer customTimeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mId = id;
        mScheduleId = scheduleId;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
    }

    public int getScheduleId() {
        return mScheduleId;
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
