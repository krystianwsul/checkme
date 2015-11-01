package com.example.krystianwsul.organizatortest.domainmodel.dates;

import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.security.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Krystian on 10/24/2015.
 */
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

    public int compareTo(TimeStamp timeStamp) {
        return mLong.compareTo(timeStamp.getLong());
    }

    public Long getLong() {
        return mLong;
    }

    public String toString() {
        return getDate().toString() + " " + getHourMinute().toString();
    }
}
