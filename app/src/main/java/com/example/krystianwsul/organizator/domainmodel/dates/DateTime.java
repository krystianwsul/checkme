package com.example.krystianwsul.organizator.domainmodel.dates;

import android.content.Context;
import android.support.annotation.NonNull;

import com.example.krystianwsul.organizator.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizator.domainmodel.times.Time;

import junit.framework.Assert;

public class DateTime implements Comparable<DateTime> {
    private final Date mDate;
    private final Time mTime;

    public DateTime(Date date, Time time) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(time != null);
        Assert.assertTrue(time.getHourMinute(date.getDayOfWeek()) != null);

        mDate = date;
        mTime = time;
    }

    public Date getDate() {
        return mDate;
    }

    public Time getTime() {
        return mTime;
    }

    public int compareTo(@NonNull DateTime dateTime) {
        int dateComparison = mDate.compareTo(dateTime.getDate());
        if (dateComparison != 0)
            return dateComparison;

        DayOfWeek day = mDate.getDayOfWeek();
        HourMinute myHourMinute = mTime.getHourMinute(day);
        Assert.assertTrue(myHourMinute != null);

        HourMinute otherHourMinute = dateTime.getTime().getHourMinute(day);
        Assert.assertTrue(otherHourMinute != null);

        return myHourMinute.compareTo(otherHourMinute);
    }

    public boolean equals(DateTime dateTime) {
        Assert.assertTrue(dateTime != null);
        return (compareTo(dateTime) == 0);
    }

    public String toString() {
        return mDate.toString() + " " + mTime.toString();
    }

    public String getDisplayText(Context context) {
        return mDate.getDisplayText(context) + ", " + mTime.toString();
    }

    public TimeStamp getTimeStamp() {
        return new TimeStamp(mDate, mTime.getHourMinute(mDate.getDayOfWeek()));
    }
}
