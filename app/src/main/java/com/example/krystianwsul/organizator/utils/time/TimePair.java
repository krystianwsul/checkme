package com.example.krystianwsul.organizator.utils.time;

import android.os.Parcel;
import android.os.Parcelable;

import junit.framework.Assert;

public class TimePair implements Parcelable {
    public final Integer CustomTimeId;
    public final HourMinute HourMinute;

    public TimePair(Integer customTimeId, HourMinute hourMinute) {
        Assert.assertTrue((customTimeId == null) != (hourMinute == null));

        CustomTimeId = customTimeId;
        HourMinute = hourMinute;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (CustomTimeId != null) {
            out.writeInt(1);
            out.writeInt(CustomTimeId);
        } else {
            out.writeInt(2);
            out.writeParcelable(HourMinute, 0);
        }
    }

    public static final Parcelable.Creator<TimePair> CREATOR = new Creator<TimePair>() {
        @Override
        public TimePair createFromParcel(Parcel source) {
            if (source.readInt() == 1) {
                return new TimePair(source.readInt(), null);
            } else {
                return new TimePair(null, (HourMinute) source.readParcelable(com.example.krystianwsul.organizator.utils.time.HourMinute.class.getClassLoader()));
            }
        }

        @Override
        public TimePair[] newArray(int size) {
            return new TimePair[size];
        }
    };
}
