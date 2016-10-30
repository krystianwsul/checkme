package com.krystianwsul.checkme.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.io.Serializable;

public class InstanceKey implements Parcelable, Serializable {
    @NonNull
    public final TaskKey mTaskKey;

    @NonNull
    public final Date ScheduleDate;

    @NonNull
    public final TimePair ScheduleTimePair;

    public InstanceKey(@NonNull TaskKey taskKey, @NonNull Date scheduleDate, @NonNull TimePair scheduleTimePair) {
        mTaskKey = taskKey;
        ScheduleDate = scheduleDate;
        ScheduleTimePair = scheduleTimePair;
    }

    @Override
    public int hashCode() {
        return mTaskKey.hashCode() + ScheduleDate.hashCode() + ScheduleTimePair.hashCode();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object object) {
        if (object == null)
            return false;

        if (!(object instanceof InstanceKey))
            return false;

        if (object == this)
            return true;

        InstanceKey instanceKey = (InstanceKey) object;

        if (!mTaskKey.equals(instanceKey.mTaskKey))
            return false;

        if (!ScheduleDate.equals(instanceKey.ScheduleDate))
            return false;

        if (!ScheduleTimePair.equals(instanceKey.ScheduleTimePair))
            return false;

        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mTaskKey, 0);
        out.writeParcelable(ScheduleDate, 0);
        out.writeParcelable(ScheduleTimePair, 0);
    }

    public static final Parcelable.Creator<InstanceKey> CREATOR = new Creator<InstanceKey>() {
        @Override
        public InstanceKey createFromParcel(Parcel source) {
            TaskKey taskKey = source.readParcelable(TaskKey.class.getClassLoader());
            Assert.assertTrue(taskKey != null);

            Date scheduleDate = source.readParcelable(Date.class.getClassLoader());
            Assert.assertTrue(scheduleDate != null);

            TimePair scheduleTimePair = source.readParcelable(TimePair.class.getClassLoader());
            Assert.assertTrue(scheduleTimePair != null);

            return new InstanceKey(taskKey, scheduleDate, scheduleTimePair);
        }

        @Override
        public InstanceKey[] newArray(int size) {
            return new InstanceKey[size];
        }
    };
}
