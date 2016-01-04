package com.example.krystianwsul.organizator.domainmodel.dates;

import android.support.annotation.NonNull;

import com.example.krystianwsul.organizator.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class TimeStamp implements Comparable<TimeStamp> {
    private final Long mLong;

    public TimeStamp(Date date, HourMinute hourMinute) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(hourMinute != null);
        mLong = new GregorianCalendar(date.getYear(), date.getMonth() - 1, date.getDay(), hourMinute.getHour(), hourMinute.getMinute()).getTimeInMillis();
    }

    public TimeStamp(Calendar calendar) {
        Assert.assertTrue(calendar != null);
        mLong = calendar.getTimeInMillis();
    }

    public static TimeStamp getNow() {
        return new TimeStamp(Calendar.getInstance());
    }

    public TimeStamp(long milis) {
        mLong = milis;
    }

    public Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mLong);
        return calendar;
    }

    public Date getDate() {
        return new Date(getCalendar());
    }

    public HourMinute getHourMinute() {
        return new HourMinute(getCalendar());
    }

    public int compareTo(@NonNull TimeStamp timeStamp) {
        return mLong.compareTo(timeStamp.getLong());
    }

    public boolean equals(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);
        return (compareTo(timeStamp) == 0);
    }

    public Long getLong() {
        return mLong;
    }

    public String toString() {
        return getDate().toString() + " " + getHourMinute().toString();
    }

    public int hashCode() {
        return mLong.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof TimeStamp))
            return false;

        if (obj == this)
            return true;

        TimeStamp other = (TimeStamp) obj;
        return mLong.equals(other.getLong());
    }
}
