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

    public int hashCode() {
        HourMinute hourMinute = mTime.getTimeByDay(mDate.getDayOfWeek());
        return mDate.getYear() + mDate.getMonth() + mDate.getDay() + hourMinute.getHour() + hourMinute.getMinute();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DateTime))
            return false;

        if (obj == this)
            return true;

        DateTime other = (DateTime) obj;

        return (mDate.compareTo(other.getDate()) == 0 && mTime.getTimeByDay(mDate.getDayOfWeek()).compareTo(other.getTime().getTimeByDay(other.mDate.getDayOfWeek())) == 0);
    }
}
