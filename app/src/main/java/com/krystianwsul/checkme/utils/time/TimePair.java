package com.krystianwsul.checkme.utils.time;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.CustomTimeKey;

import junit.framework.Assert;

import java.io.Serializable;

public class TimePair implements Parcelable, Serializable {
    @Nullable
    public final CustomTimeKey mCustomTimeKey;

    @Nullable
    public final HourMinute mHourMinute;

    public TimePair(@Nullable CustomTimeKey customTimeKey, @Nullable HourMinute hourMinute) {
        Assert.assertTrue((customTimeKey == null) != (hourMinute == null));

        mCustomTimeKey = customTimeKey;
        mHourMinute = hourMinute;
    }

    public TimePair(@NonNull CustomTimeKey customTimeKey) {
        mCustomTimeKey = customTimeKey;
        mHourMinute = null;
    }

    public TimePair(@NonNull HourMinute hourMinute) {
        mCustomTimeKey = null;
        mHourMinute = hourMinute;
    }

    @Override
    public int hashCode() {
        if (mCustomTimeKey != null) {
            Assert.assertTrue(mHourMinute == null);
            return mCustomTimeKey.hashCode();
        } else {
            Assert.assertTrue(mHourMinute != null);
            return mHourMinute.hashCode();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null)
            return false;

        if (object == this)
            return true;

        if (!(object instanceof TimePair))
            return false;

        TimePair timePair = (TimePair) object;

        if (mCustomTimeKey != null) {
            Assert.assertTrue(mHourMinute == null);
            return mCustomTimeKey.equals(timePair.mCustomTimeKey);
        } else {
            Assert.assertTrue(mHourMinute != null);
            return mHourMinute.equals(timePair.mHourMinute);
        }
    }

    @NonNull
    public TimePair copy() {
        return new TimePair(mCustomTimeKey, mHourMinute);
    }

    @Override
    public String toString() {
        if (mCustomTimeKey != null) {
            Assert.assertTrue(mHourMinute == null);

            return super.toString() + ": " + mCustomTimeKey;
        } else {
            Assert.assertTrue(mHourMinute != null);

            return super.toString() + ": " + mHourMinute;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (mCustomTimeKey != null) {
            out.writeInt(1);
            out.writeParcelable(mCustomTimeKey, 0);
        } else {
            out.writeInt(2);
            out.writeParcelable(mHourMinute, 0);
        }
    }

    public static final Parcelable.Creator<TimePair> CREATOR = new Creator<TimePair>() {
        @Override
        public TimePair createFromParcel(Parcel source) {
            if (source.readInt() == 1) {
                return new TimePair(source.readParcelable(CustomTimeKey.class.getClassLoader()), null);
            } else {
                return new TimePair(null, source.readParcelable(HourMinute.class.getClassLoader()));
            }
        }

        @Override
        public TimePair[] newArray(int size) {
            return new TimePair[size];
        }
    };
}
