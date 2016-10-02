package com.krystianwsul.checkme.utils.time;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import junit.framework.Assert;

import java.io.Serializable;

public class TimePair implements Parcelable, Serializable {
    @Nullable
    public final Integer mCustomTimeId;

    @Nullable
    public final HourMinute mHourMinute;

    public TimePair(@Nullable Integer customTimeId, @Nullable HourMinute hourMinute) {
        Assert.assertTrue((customTimeId == null) != (hourMinute == null));

        mCustomTimeId = customTimeId;
        mHourMinute = hourMinute;
    }

    public TimePair(int customTimeId) {
        mCustomTimeId = customTimeId;
        mHourMinute = null;
    }

    public TimePair(@NonNull HourMinute hourMinute) {
        mCustomTimeId = null;
        mHourMinute = hourMinute;
    }

    @Override
    public int hashCode() {
        if (mCustomTimeId != null) {
            Assert.assertTrue(mHourMinute == null);
            return mCustomTimeId.hashCode();
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

        if (mCustomTimeId != null) {
            Assert.assertTrue(mHourMinute == null);
            return mCustomTimeId.equals(timePair.mCustomTimeId);
        } else {
            Assert.assertTrue(mHourMinute != null);
            return mHourMinute.equals(timePair.mHourMinute);
        }
    }

    @NonNull
    public TimePair copy() {
        return new TimePair(mCustomTimeId, mHourMinute);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (mCustomTimeId != null) {
            out.writeInt(1);
            out.writeInt(mCustomTimeId);
        } else {
            out.writeInt(2);
            out.writeParcelable(mHourMinute, 0);
        }
    }

    public static final Parcelable.Creator<TimePair> CREATOR = new Creator<TimePair>() {
        @Override
        public TimePair createFromParcel(Parcel source) {
            if (source.readInt() == 1) {
                return new TimePair(source.readInt(), null);
            } else {
                return new TimePair(null, source.readParcelable(com.krystianwsul.checkme.utils.time.HourMinute.class.getClassLoader()));
            }
        }

        @Override
        public TimePair[] newArray(int size) {
            return new TimePair[size];
        }
    };
}
