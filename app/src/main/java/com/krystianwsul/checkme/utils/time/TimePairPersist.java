package com.krystianwsul.checkme.utils.time;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.CustomTimeKey;

import junit.framework.Assert;

public class TimePairPersist implements Parcelable {
    @Nullable
    private CustomTimeKey mCustomTimeKey;

    @NonNull
    private HourMinute mHourMinute;

    public TimePairPersist(@NonNull HourMinute hourMinute) {
        mHourMinute = hourMinute;
    }

    private TimePairPersist(@Nullable CustomTimeKey customTimeKey, @NonNull HourMinute hourMinute) {
        mCustomTimeKey = customTimeKey;
        mHourMinute = hourMinute;
    }

    public TimePairPersist(@NonNull TimePair timePair) {
        mCustomTimeKey = timePair.mCustomTimeKey;

        if (timePair.mHourMinute != null)
            mHourMinute = timePair.mHourMinute;
        else
            mHourMinute = HourMinute.getNextHour().getSecond();
    }

    public void setCustomTimeKey(@NonNull CustomTimeKey customTimeKey) {
        mCustomTimeKey = customTimeKey;
    }

    public void setHourMinute(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        mCustomTimeKey = null;
        mHourMinute = hourMinute;
    }

    @NonNull
    public HourMinute getHourMinute() {
        return mHourMinute;
    }

    @Nullable
    public CustomTimeKey getCustomTimeKey() {
        return mCustomTimeKey;
    }

    @NonNull
    public TimePair getTimePair() {
        if (mCustomTimeKey != null)
            return new TimePair(mCustomTimeKey);
        else
            return new TimePair(mHourMinute);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mHourMinute, 0);
        if (mCustomTimeKey != null) {
            dest.writeInt(1);
            dest.writeParcelable(mCustomTimeKey, 0);
        } else {
            dest.writeInt(0);
        }
    }

    public static Parcelable.Creator<TimePairPersist> CREATOR = new Creator<TimePairPersist>() {
        @Override
        public TimePairPersist createFromParcel(Parcel source) {
            HourMinute hourMinute = source.readParcelable(HourMinute.class.getClassLoader());
            Assert.assertTrue(hourMinute != null);

            CustomTimeKey customTimeKey;
            if (source.readInt() == 1) {
                customTimeKey = source.readParcelable(CustomTimeKey.class.getClassLoader());
                Assert.assertTrue(customTimeKey != null);
            } else {
                customTimeKey = null;
            }

            return new TimePairPersist(customTimeKey, hourMinute);
        }

        @Override
        public TimePairPersist[] newArray(int size) {
            return new TimePairPersist[size];
        }
    };
}
