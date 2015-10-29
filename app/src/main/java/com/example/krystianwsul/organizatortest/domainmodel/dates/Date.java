package com.example.krystianwsul.organizatortest.domainmodel.dates;

import android.content.Context;

import com.example.krystianwsul.organizatortest.R;

import junit.framework.Assert;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Krystian on 10/17/2015.
 */
public class Date implements Comparable<Date> {
    private final Integer mYear;
    private final Integer mMonth;
    private final Integer mDay;

    public static Date today() {
        return new Date(Calendar.getInstance());
    }

    public Date(Integer year, Integer month, Integer day) {
        mYear = year;
        mMonth = month;
        mDay = day;
    }

    public Date(Calendar calendar) {
        Assert.assertTrue(calendar != null);

        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH) + 1;
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
    }

    public Integer getYear() {
        return mYear;
    }

    public Integer getMonth() {
        return mMonth;
    }

    public Integer getDay() {
        return mDay;
    }

    public int compareTo(Date date) {
        Assert.assertTrue(date != null);

        int yearComparison = mYear.compareTo(date.getYear());
        if (yearComparison != 0)
            return yearComparison;

        int monthComparison = mMonth.compareTo(date.getMonth());
        if (monthComparison != 0)
            return monthComparison;

        return mDay.compareTo(date.getDay());
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.getDayFromCalendar(new GregorianCalendar(mYear, mMonth - 1, mDay));
    }

    public String toString() {
        return String.format("%04d", mYear) + "-" + String.format("%02d", mMonth) + "-" +String.format("%02d", mDay);
    }

    public String getDisplayText(Context context) {
        Calendar todayCalendar = Calendar.getInstance();
        Date todayDate = new Date(todayCalendar);

        Calendar yesterdayCalendar = Calendar.getInstance();
        yesterdayCalendar.add(Calendar.DATE, -1);
        Date yesterdayDate = new Date(yesterdayCalendar);

        Calendar tomorrowCalendar = Calendar.getInstance();
        tomorrowCalendar.add(Calendar.DATE, -1);
        Date tomorrowDate = new Date(tomorrowCalendar);

        if (this.compareTo(todayDate) == 0)
            return context.getString(R.string.today);
        else if (this.compareTo(yesterdayDate) == 0)
            return context.getString(R.string.yesterday);
        else if (this.compareTo(tomorrowDate) == 0)
            return context.getString(R.string.tomorrow);
        else
            return getDayOfWeek().toString() + ", " + toString();
    }
}
