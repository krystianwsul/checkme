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
    public final ScheduleKey mScheduleKey;

    public InstanceKey(@NonNull TaskKey taskKey, @NonNull Date scheduleDate, @NonNull TimePair scheduleTimePair) {
        mTaskKey = taskKey;
        mScheduleKey = new ScheduleKey(scheduleDate, scheduleTimePair);
    }

    private InstanceKey(@NonNull TaskKey taskKey, @NonNull ScheduleKey scheduleKey) {
        mTaskKey = taskKey;
        mScheduleKey = scheduleKey;
    }

    @Override
    public int hashCode() {
        return mTaskKey.hashCode() + mScheduleKey.hashCode();
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

        if (!mScheduleKey.equals(instanceKey.mScheduleKey))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + mTaskKey + ", " + mScheduleKey;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mTaskKey, 0);
        out.writeParcelable(mScheduleKey, 0);
    }

    public static final Parcelable.Creator<InstanceKey> CREATOR = new Creator<InstanceKey>() {
        @Override
        public InstanceKey createFromParcel(Parcel source) {
            TaskKey taskKey = source.readParcelable(TaskKey.class.getClassLoader());
            Assert.assertTrue(taskKey != null);

            ScheduleKey scheduleKey = source.readParcelable(Date.class.getClassLoader());
            Assert.assertTrue(scheduleKey != null);

            return new InstanceKey(taskKey, scheduleKey);
        }

        @Override
        public InstanceKey[] newArray(int size) {
            return new InstanceKey[size];
        }
    };
}
