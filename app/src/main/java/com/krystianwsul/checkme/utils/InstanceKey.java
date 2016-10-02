package com.krystianwsul.checkme.utils;

import android.os.Parcel;
import android.os.Parcelable;

import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.io.Serializable;

public class InstanceKey implements Parcelable, Serializable {
    public final int TaskId;
    public final Date ScheduleDate;
    public final TimePair ScheduleTimePair;

    public InstanceKey(int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute) {
        Assert.assertTrue(scheduleDate != null);
        Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

        TaskId = taskId;
        ScheduleDate = scheduleDate;
        ScheduleTimePair = new TimePair(scheduleCustomTimeId, scheduleHourMinute);
    }

    @Override
    public int hashCode() {
        return TaskId + ScheduleDate.hashCode() + (ScheduleTimePair.mCustomTimeId != null ? ScheduleTimePair.mCustomTimeId : 0) + (ScheduleTimePair.mHourMinute != null ? ScheduleTimePair.mHourMinute.hashCode() : 0);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null)
            return false;

        if (!(object instanceof InstanceKey))
            return false;

        if (object == this)
            return true;

        InstanceKey instanceKey = (InstanceKey) object;

        if (TaskId != instanceKey.TaskId)
            return false;

        if (!ScheduleDate.equals(instanceKey.ScheduleDate))
            return false;

        if (ScheduleTimePair.mCustomTimeId == null) {
            Assert.assertTrue(ScheduleTimePair.mHourMinute != null);

            return (instanceKey.ScheduleTimePair.mCustomTimeId == null && ScheduleTimePair.mHourMinute.equals(instanceKey.ScheduleTimePair.mHourMinute));
        } else {
            Assert.assertTrue(ScheduleTimePair.mHourMinute == null);

            return (instanceKey.ScheduleTimePair.mCustomTimeId != null && ScheduleTimePair.mCustomTimeId.equals(instanceKey.ScheduleTimePair.mCustomTimeId));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(TaskId);
        out.writeParcelable(ScheduleDate, 0);
        out.writeParcelable(ScheduleTimePair, 0);
    }

    public static final Parcelable.Creator<InstanceKey> CREATOR = new Creator<InstanceKey>() {
        @Override
        public InstanceKey createFromParcel(Parcel source) {
            int taskId = source.readInt();

            Date scheduleDate = source.readParcelable(Date.class.getClassLoader());
            Assert.assertTrue(scheduleDate != null);

            TimePair scheduleTimePair = source.readParcelable(TimePair.class.getClassLoader());
            Assert.assertTrue(scheduleTimePair != null);

            Integer scheduleCustomTimeId = scheduleTimePair.mCustomTimeId;
            HourMinute scheduleHourMinute = scheduleTimePair.mHourMinute;
            Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

            return new InstanceKey(taskId, scheduleDate, scheduleCustomTimeId, scheduleHourMinute);
        }

        @Override
        public InstanceKey[] newArray(int size) {
            return new InstanceKey[size];
        }
    };
}
