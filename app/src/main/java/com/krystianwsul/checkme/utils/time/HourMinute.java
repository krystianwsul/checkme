package com.krystianwsul.checkme.utils.time;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class HourMinute implements Comparable<HourMinute>, Parcelable, Serializable {
    private final int mHour;
    private final int mMinute;

    public static HourMinute getNow() {
        return TimeStamp.getNow().getHourMinute();
    }

    public static HourMinute getNextHour() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        return new HourMinute(calendar);
    }

    public HourMinute(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
    }

    public HourMinute(Calendar calendar) {
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
        int comparisonHour = Integer.valueOf(mHour).compareTo(hourMinute.getHour());

        if (comparisonHour != 0)
            return comparisonHour;

        return Integer.valueOf(mMinute).compareTo(hourMinute.getMinute());
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
        Date date = TimeStamp.getNow().getDate();
        java.util.Date javaDate = new java.util.Date(date.getYear(), date.getMonth(), date.getDay(), mHour, mMinute);
        return SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(javaDate);
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
