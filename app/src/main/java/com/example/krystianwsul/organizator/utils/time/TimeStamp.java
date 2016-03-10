package com.example.krystianwsul.organizator.utils.time;

import android.support.annotation.NonNull;

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

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        mLong = new GregorianCalendar(date.getYear(), date.getMonth() - 1, date.getDay(), hourMinute.getHour(), hourMinute.getMinute()).getTimeInMillis();
    }

    public static TimeStamp getNow() {
        return new TimeStamp(Calendar.getInstance());
    }

    public TimeStamp(long milis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milis);
        calendar.set(Calendar.MILLISECOND ,0);
        mLong = calendar.getTimeInMillis();
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

    public Long getLong() {
        return mLong;
    }

    public String toString() {
        return getDate().toString() + " " + getHourMinute().toString();
    }

    public int hashCode() {
        return mLong.hashCode();
    }

    public boolean equals(Object object) {
        if (object == null)
            return false;

        if (!(object instanceof TimeStamp))
            return false;

        if (object == this)
            return true;

        TimeStamp other = (TimeStamp) object;
        return mLong.equals(other.getLong());
    }

    public ExactTimeStamp toExactTimeStamp() {
        return new ExactTimeStamp(mLong);
    }
}
