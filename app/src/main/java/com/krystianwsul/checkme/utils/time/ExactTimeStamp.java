package com.krystianwsul.checkme.utils.time;

import android.support.annotation.NonNull;

import junit.framework.Assert;

import java.util.Calendar;

public class ExactTimeStamp implements Comparable<ExactTimeStamp> {
    private final Long mLong;

    public ExactTimeStamp(Date date, HourMilli hourMilli) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(hourMilli != null);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonth() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
        calendar.set(Calendar.HOUR_OF_DAY, hourMilli.getHour());
        calendar.set(Calendar.MINUTE, hourMilli.getMinute());
        calendar.set(Calendar.SECOND, hourMilli.getSecond());
        calendar.set(Calendar.MILLISECOND, hourMilli.getMilli());
        mLong = calendar.getTimeInMillis();
    }

    public ExactTimeStamp(Calendar calendar) {
        Assert.assertTrue(calendar != null);
        mLong = calendar.getTimeInMillis();
    }

    @NonNull
    public static ExactTimeStamp getNow() {
        return new ExactTimeStamp(Calendar.getInstance());
    }

    public ExactTimeStamp(long millis) {
        mLong = millis;
    }

    @NonNull
    public Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mLong);
        return calendar;
    }

    @NonNull
    public Date getDate() {
        return new Date(getCalendar());
    }

    public HourMilli getHourMilli() {
        return new HourMilli(getCalendar());
    }

    public int compareTo(@NonNull ExactTimeStamp exactTimeStamp) {
        return mLong.compareTo(exactTimeStamp.getLong());
    }

    @NonNull
    public Long getLong() {
        return mLong;
    }

    /*
    public String toString() {
        return getDate().toString() + " " + getHourMinute().toString();
    }
    */

    public int hashCode() {
        return mLong.hashCode();
    }

    public boolean equals(Object object) {
        if (object == null)
            return false;

        if (!(object instanceof ExactTimeStamp))
            return false;

        if (object == this)
            return true;

        ExactTimeStamp other = (ExactTimeStamp) object;
        return mLong.equals(other.getLong());
    }

    @NonNull
    public ExactTimeStamp plusOne() {
        return new ExactTimeStamp(mLong + 1);
    }

    public ExactTimeStamp minusOne() {
        return new ExactTimeStamp(mLong - 1);
    }

    public TimeStamp toTimeStamp() {
        return TimeStamp.Companion.fromMillis(mLong);
    }

    public String toString() {
        return getDate().toString() + " " + getHourMilli().toString();
    }
}
