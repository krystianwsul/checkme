package com.example.krystianwsul.organizatortest.domainmodel.dates;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/18/2015.
 */
public class DateTime implements Comparable<DateTime> {
    private Date mDate;
    private Time mTime;

    public DateTime(Date date, Time time) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(time != null);
        Assert.assertTrue(time.getTimeByDay(date.getDayOfWeek()) != null);

        mDate = date;
        mTime = time;
    }

    public Date getDate() {
        return mDate;
    }

    public Time getTime() {
        return mTime;
    }

    public int compareTo(DateTime dateTime) {
        int dateComparison = mDate.compareTo(dateTime.getDate());
        if (dateComparison != 0)
            return dateComparison;

        DayOfWeek day = mDate.getDayOfWeek();
        HourMinute myHourMinute = mTime.getTimeByDay(day);
        HourMinute otherHourMinute = dateTime.getTime().getTimeByDay(day);

        Assert.assertTrue(myHourMinute != null);
        Assert.assertTrue(otherHourMinute != null);

        int x = myHourMinute.compareTo(otherHourMinute);
        return myHourMinute.compareTo(otherHourMinute);
    }

    public String toString() {
        return mDate.toString() + " " + mTime.toString();
    }

    public String getDisplayText(Context context) {
        return mDate.getDisplayText(context) + ", " + mTime.toString();
    }

    public TimeStamp getTimeStamp() {
        return new TimeStamp(mDate, mTime.getTimeByDay(mDate.getDayOfWeek()));
    }
}
