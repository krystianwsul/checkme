package com.example.krystianwsul.organizator.utils.time;

import android.support.annotation.NonNull;

import junit.framework.Assert;

import java.util.Calendar;

public class ExactTimeStamp implements Comparable<ExactTimeStamp> {
    private final Long mLong;

    public ExactTimeStamp(Date date, HourMili hourMili) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(hourMili != null);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonth() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
        calendar.set(Calendar.HOUR_OF_DAY, hourMili.getHour());
        calendar.set(Calendar.MINUTE, hourMili.getMinute());
        calendar.set(Calendar.SECOND, hourMili.getSecond());
        calendar.set(Calendar.MILLISECOND, hourMili.getMili());
        mLong = calendar.getTimeInMillis();
    }

    public ExactTimeStamp(Calendar calendar) {
        Assert.assertTrue(calendar != null);
        mLong = calendar.getTimeInMillis();
    }

    public static ExactTimeStamp getNow() {
        return new ExactTimeStamp(Calendar.getInstance());
    }

    public ExactTimeStamp(long milis) {
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

    public HourMili getHourMili() {
        return new HourMili(getCalendar());
    }

    public int compareTo(@NonNull ExactTimeStamp exactTimeStamp) {
        return mLong.compareTo(exactTimeStamp.getLong());
    }

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

    public ExactTimeStamp plusOne() {
        return new ExactTimeStamp(mLong + 1);
    }

    public TimeStamp toTimeStamp() {
        return new TimeStamp(mLong);
    }

    public String toString() {
        return getDate().toString() + " " + getHourMili().toString();
    }
}
