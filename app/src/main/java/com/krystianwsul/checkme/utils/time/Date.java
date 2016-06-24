package com.krystianwsul.checkme.utils.time;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.R;

import junit.framework.Assert;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Date implements Comparable<Date>, Parcelable, Serializable {
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

    public int compareTo(@NonNull Date date) {
        int yearComparison = mYear.compareTo(date.getYear());
        if (yearComparison != 0)
            return yearComparison;

        int monthComparison = mMonth.compareTo(date.getMonth());
        if (monthComparison != 0)
            return monthComparison;

        return mDay.compareTo(date.getDay());
    }

    @Override
    public int hashCode() {
        return mYear + mMonth + mDay;
    }

    @Override
    public boolean equals(Object object) {
        return ((object != null) && (object instanceof Date) && ((object == this) || (compareTo((Date) object) == 0)));
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.getDayFromCalendar(new GregorianCalendar(mYear, mMonth - 1, mDay));
    }

    @SuppressWarnings({"deprecation"})
    public String toString() {
        java.util.Date javaDate = new java.util.Date(mYear, mMonth - 1, mDay);
        return SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(javaDate);
    }

    public String getDisplayText(Context context) {
        Calendar todayCalendar = Calendar.getInstance();
        Date todayDate = new Date(todayCalendar);

        Calendar yesterdayCalendar = Calendar.getInstance();
        yesterdayCalendar.add(Calendar.DATE, -1);
        Date yesterdayDate = new Date(yesterdayCalendar);

        Calendar tomorrowCalendar = Calendar.getInstance();
        tomorrowCalendar.add(Calendar.DATE, 1);
        Date tomorrowDate = new Date(tomorrowCalendar);

        if (this.equals(todayDate))
            return context.getString(R.string.today);
        else if (this.equals(yesterdayDate))
            return context.getString(R.string.yesterday);
        else if (this.equals(tomorrowDate))
            return context.getString(R.string.tomorrow);
        else
            return getDayOfWeek().toString() + ", " + toString();
    }

    public Calendar getCalendar() {
        return new GregorianCalendar(mYear, mMonth - 1, mDay);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mYear);
        out.writeInt(mMonth);
        out.writeInt(mDay);
    }

    public static final Parcelable.Creator<Date> CREATOR = new Creator<Date>() {
        @Override
        public Date createFromParcel(Parcel in) {
            int year = in.readInt();
            int month = in.readInt();
            int day = in.readInt();

            return new Date(year, month, day);
        }

        @Override
        public Date[] newArray(int size) {
            return new Date[size];
        }
    };
}
