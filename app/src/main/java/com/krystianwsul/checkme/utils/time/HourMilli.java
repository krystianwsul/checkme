package com.krystianwsul.checkme.utils.time;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Calendar;

public class HourMilli implements Comparable<HourMilli>, Parcelable {
    private final int mHour;
    private final int mMinute;
    private final int mSecond;
    private final int mMilli;

    public HourMilli(int hour, int minute, int second, int milli) {
        mHour = hour;
        mMinute = minute;
        mSecond = second;
        mMilli = milli;
    }

    HourMilli(Calendar calendar) {
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
        mSecond = calendar.get(Calendar.SECOND);
        mMilli = calendar.get(Calendar.MILLISECOND);
    }

    public int getHour() {
        return mHour;
    }

    public int getMinute() {
        return mMinute;
    }

    int getSecond() {
        return mSecond;
    }

    int getMilli() {
        return mMilli;
    }

    public int compareTo(@NonNull HourMilli hourMilli) {
        int comparisonHour = Integer.valueOf(mHour).compareTo(hourMilli.getHour());

        if (comparisonHour != 0)
            return comparisonHour;

        int comparisonMinute = Integer.valueOf(mMinute).compareTo(hourMilli.getMinute());

        if (comparisonMinute != 0)
            return comparisonMinute;

        int comparisonSecond = Integer.valueOf(mSecond).compareTo(hourMilli.getSecond());

        if (comparisonSecond != 0)
            return comparisonSecond;

        return Integer.valueOf(mMilli).compareTo(hourMilli.getMilli());
    }

    @Override
    public int hashCode() {
        return mHour + mMinute + mSecond + mMilli;
    }

    @Override
    public boolean equals(Object object) {
        return ((object != null) && (object instanceof HourMilli) && (object == this || compareTo((HourMilli) object) == 0));
    }

    @SuppressLint("DefaultLocale")
    public String toString() {
        return String.format("%02d", mHour) + ":" + String.format("%02d", mMinute) + ":" + String.format("%02d", mSecond) + ":" + String.format("%03d", mMilli);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mHour);
        out.writeInt(mMinute);
        out.writeInt(mSecond);
        out.writeInt(mMilli);
    }

    public static final Parcelable.Creator<HourMilli> CREATOR = new Creator<HourMilli>() {
        @Override
        public HourMilli createFromParcel(Parcel source) {
            int hour = source.readInt();
            int minute = source.readInt();
            int second = source.readInt();
            int milli = source.readInt();

            return new HourMilli(hour, minute, second, milli);
        }

        @Override
        public HourMilli[] newArray(int size) {
            return new HourMilli[size];
        }
    };
}
