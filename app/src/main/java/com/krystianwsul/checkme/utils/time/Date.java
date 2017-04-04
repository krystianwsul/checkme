package com.krystianwsul.checkme.utils.time;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.R;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Date implements Comparable<Date>, Parcelable, Serializable {
    private final int mYear;
    private final int mMonth;
    private final int mDay;

    public static Date today() {
        return new Date(Calendar.getInstance());
    }

    public Date(int year, int month, int day) {
        mYear = year;
        mMonth = month;
        mDay = day;
    }

    public Date(@NonNull Calendar calendar) {
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH) + 1;
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
    }

    public int getYear() {
        return mYear;
    }

    public int getMonth() {
        return mMonth;
    }

    public int getDay() {
        return mDay;
    }

    public int compareTo(@NonNull Date date) {
        int yearComparison = Integer.valueOf(mYear).compareTo(date.getYear());
        if (yearComparison != 0)
            return yearComparison;

        int monthComparison = Integer.valueOf(mMonth).compareTo(date.getMonth());
        if (monthComparison != 0)
            return monthComparison;

        return Integer.valueOf(mDay).compareTo(date.getDay());
    }

    @Override
    public int hashCode() {
        return mYear + mMonth + mDay;
    }

    @Override
    public boolean equals(Object object) {
        return ((object != null) && (object instanceof Date) && ((object == this) || (compareTo((Date) object) == 0)));
    }

    @NonNull
    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.getDayFromCalendar(new GregorianCalendar(mYear, mMonth - 1, mDay));
    }

    @NonNull
    public String toString() {
        return DateTimeFormat.forStyle("S-").print(new LocalDate(mYear, mMonth, mDay));
    }

    @NonNull
    public String getDisplayText(@NonNull Context context) {
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

    @NonNull
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
