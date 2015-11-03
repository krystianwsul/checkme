package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class SingleScheduleRecord {
    private final int mId;

    private final int mYear;
    private final int mMonth;
    private final int mDay;

    private final Integer mTimeRecordId;

    private final Integer mHour;
    private final Integer mMinute;

    public SingleScheduleRecord(int id, int year, int month, int day, Integer timeRecordId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (timeRecordId == null));
        Assert.assertTrue((hour != null) || (timeRecordId != null));

        mId = id;

        mYear = year;
        mMonth = month;
        mDay = day;

        mTimeRecordId = timeRecordId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
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

    public Integer getTimeRecordId() {
        return mTimeRecordId;
    }

    public Integer getHour() {
        return mHour;
    }

    public Integer getMinute() {
        return mMinute;
    }
}
