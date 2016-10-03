package com.krystianwsul.checkme.utils.time;

import android.content.Context;
import android.support.annotation.NonNull;

import junit.framework.Assert;

public class DateTime implements Comparable<DateTime> {
    @NonNull
    private final Date mDate;

    @NonNull
    private final Time mTime;

    public DateTime(@NonNull Date date, @NonNull Time time) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(time != null);
        Assert.assertTrue(time.getHourMinute(date.getDayOfWeek()) != null);

        mDate = date;
        mTime = time;
    }

    @NonNull
    public Date getDate() {
        return mDate;
    }

    @NonNull
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

    @Override
    public int hashCode() {
        return mDate.hashCode() + mTime.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return ((object != null) && (object instanceof DateTime) && (object == this || compareTo((DateTime) object) == 0));
    }

    @NonNull
    public String toString() {
        return mDate.toString() + " " + mTime.toString();
    }

    @NonNull
    public String getDisplayText(Context context) {
        return mDate.getDisplayText(context) + ", " + mTime.toString();
    }

    @NonNull
    public TimeStamp getTimeStamp() {
        return new TimeStamp(mDate, mTime.getHourMinute(mDate.getDayOfWeek()));
    }
}
