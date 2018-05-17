package com.krystianwsul.checkme.utils.time;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import java.io.Serializable;
import java.util.Calendar;

import kotlin.Pair;

public class HourMinute implements Comparable<HourMinute>, Parcelable, Serializable {
    private final int mHour;
    private final int mMinute;

    @NonNull
    public static HourMinute getNow() {
        return TimeStamp.getNow().getHourMinute();
    }

    @NonNull
    public static Pair<Date, HourMinute> getNextHour() {
        return getNextHour(Date.Companion.today(), ExactTimeStamp.getNow());
    }

    @NonNull
    public static Pair<Date, HourMinute> getNextHour(@NonNull Date date) {
        return getNextHour(date, ExactTimeStamp.getNow());
    }

    @NonNull
    static Pair<Date, HourMinute> getNextHour(@NonNull Date date, @NonNull ExactTimeStamp now) {
        HourMinute hourMinute = now.toTimeStamp().getHourMinute();

        Calendar calendar = date.getCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, hourMinute.getHour());
        calendar.set(Calendar.MINUTE, hourMinute.getMinute());

        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);

        return new Pair<>(new Date(calendar), new HourMinute(calendar));
    }

    public HourMinute(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
    }

    public HourMinute(@NonNull Calendar calendar) {
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
    }

    public int getHour() {
        return mHour;
    }

    public int getMinute() {
        return mMinute;
    }

    public int compareTo(@NonNull HourMinute hourMinute) {
        int comparisonHour = Integer.compare(mHour, hourMinute.getHour());

        if (comparisonHour != 0)
            return comparisonHour;

        return Integer.compare(mMinute, hourMinute.getMinute());
    }

    @Override
    public int hashCode() {
        return mHour + mMinute;
    }

    @Override
    public boolean equals(Object object) {
        return ((object != null) && (object instanceof HourMinute) && (object == this || compareTo((HourMinute) object) == 0));
    }

    @SuppressWarnings({"deprecation"})
    public String toString() {
        return DateTimeFormat.forStyle("-S").print(new LocalTime(mHour, mMinute));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mHour);
        out.writeInt(mMinute);
    }

    public static final Parcelable.Creator<HourMinute> CREATOR = new Creator<HourMinute>() {
        @Override
        public HourMinute createFromParcel(Parcel source) {
            int hour = source.readInt();
            int minute = source.readInt();

            return new HourMinute(hour, minute);
        }

        @Override
        public HourMinute[] newArray(int size) {
            return new HourMinute[size];
        }
    };

    public HourMilli toHourMilli() {
        return new HourMilli(mHour, mMinute, 0, 0);
    }
}
