package com.example.krystianwsul.organizator.utils.time;

import android.os.Parcel;
import android.os.Parcelable;

import junit.framework.Assert;

public class TimePairPersist implements Parcelable {
    private Integer mCustomTimeId;
    private HourMinute mHourMinute = HourMinute.getNextHour();

    private TimePairPersist(Integer customTimeId, HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        mCustomTimeId = customTimeId;
        mHourMinute = hourMinute;
    }

    public TimePairPersist(TimePair timePair) {
        mCustomTimeId = timePair.CustomTimeId;
        if (timePair.HourMinute != null)
            mHourMinute = timePair.HourMinute;
    }

    public void setCustomTimeId(int customTimeId) {
        mCustomTimeId = customTimeId;
    }

    public void setHourMinute(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        mCustomTimeId = null;
        mHourMinute = hourMinute;
    }

    public HourMinute getHourMinute() {
        return mHourMinute;
    }

    public Integer getCustomTimeId() {
        return mCustomTimeId;
    }

    public TimePair getTimePair() {
        Assert.assertTrue(mHourMinute != null);
        if (mCustomTimeId != null)
            return new TimePair(mCustomTimeId);
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
        if (mCustomTimeId != null) {
            dest.writeInt(1);
            dest.writeInt(mCustomTimeId);
        } else {
            dest.writeInt(0);
        }
    }

    public static Parcelable.Creator<TimePairPersist> CREATOR = new Creator<TimePairPersist>() {
        @Override
        public TimePairPersist createFromParcel(Parcel source) {
            HourMinute hourMinute = source.readParcelable(HourMinute.class.getClassLoader());
            boolean hasCustomTimeId = (source.readInt() == 1);
            Integer customTimeId = (hasCustomTimeId ? source.readInt() : null);

            return new TimePairPersist(customTimeId, hourMinute);
        }

        @Override
        public TimePairPersist[] newArray(int size) {
            return new TimePairPersist[size];
        }
    };
}
