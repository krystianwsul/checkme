package com.example.krystianwsul.organizator.utils.time;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Calendar;

public class HourMili implements Comparable<HourMili>, Parcelable {
    private final int mHour;
    private final int mMinute;
    private final int mSecond;
    private final int mMili;

    public HourMili(int hour, int minute, int second, int mili) {
        mHour = hour;
        mMinute = minute;
        mSecond = second;
        mMili = mili;
    }

    public HourMili(Calendar calendar) {
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
        mSecond = calendar.get(Calendar.SECOND);
        mMili = calendar.get(Calendar.MILLISECOND);
    }

    public int getHour() {
        return mHour;
    }

    public int getMinute() {
        return mMinute;
    }

    public int getSecond() {
        return mSecond;
    }

    public int getMili() {
        return mMili;
    }

    public int compareTo(@NonNull HourMili hourMili) {
        int comparisonHour = Integer.valueOf(mHour).compareTo(hourMili.getHour());

        if (comparisonHour != 0)
            return comparisonHour;

        int comparisonMinute = Integer.valueOf(mMinute).compareTo(hourMili.getMinute());

        if (comparisonMinute != 0)
            return comparisonMinute;

        int comparisonSecond = Integer.valueOf(mSecond).compareTo(hourMili.getSecond());

        if (comparisonSecond != 0)
            return comparisonSecond;

        return Integer.valueOf(mMili).compareTo(hourMili.getMili());
    }

    @Override
    public int hashCode() {
        return mHour + mMinute + mSecond + mMili;
    }

    @Override
    public boolean equals(Object object) {
        return ((object != null) && (object instanceof HourMili) && (object == this || compareTo((HourMili) object) == 0));
    }

    public String toString() {
        return mHour + ":" + mMinute + ":" + mSecond + ":" + mMili;
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
        out.writeInt(mMili);
    }

    public static final Parcelable.Creator<HourMili> CREATOR = new Creator<HourMili>() {
        @Override
        public HourMili createFromParcel(Parcel source) {
            int hour = source.readInt();
            int minute = source.readInt();
            int second = source.readInt();
            int mili = source.readInt();

            return new HourMili(hour, minute, second, mili);
        }

        @Override
        public HourMili[] newArray(int size) {
            return new HourMili[size];
        }
    };
}
