package com.krystianwsul.checkme.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.io.Serializable;

public class ScheduleKey implements Parcelable, Serializable {
    @NonNull
    public final Date ScheduleDate;

    @NonNull
    public final TimePair ScheduleTimePair;

    ScheduleKey(@NonNull Date scheduleDate, @NonNull TimePair scheduleTimePair) {
        ScheduleDate = scheduleDate;
        ScheduleTimePair = scheduleTimePair;
    }

    @Override
    public int hashCode() {
        return ScheduleDate.hashCode() + ScheduleTimePair.hashCode();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object object) {
        if (object == null)
            return false;

        if (!(object instanceof ScheduleKey))
            return false;

        if (object == this)
            return true;

        ScheduleKey scheduleKey = (ScheduleKey) object;

        if (!ScheduleDate.equals(scheduleKey.ScheduleDate))
            return false;

        if (!ScheduleTimePair.equals(scheduleKey.ScheduleTimePair))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + ScheduleDate + ", " + ScheduleTimePair;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(ScheduleDate, 0);
        out.writeParcelable(ScheduleTimePair, 0);
    }

    public static final Creator<ScheduleKey> CREATOR = new Creator<ScheduleKey>() {
        @Override
        public ScheduleKey createFromParcel(Parcel source) {
            Date scheduleDate = source.readParcelable(Date.class.getClassLoader());
            Assert.assertTrue(scheduleDate != null);

            TimePair scheduleTimePair = source.readParcelable(TimePair.class.getClassLoader());
            Assert.assertTrue(scheduleTimePair != null);

            return new ScheduleKey(scheduleDate, scheduleTimePair);
        }

        @Override
        public ScheduleKey[] newArray(int size) {
            return new ScheduleKey[size];
        }
    };
}
