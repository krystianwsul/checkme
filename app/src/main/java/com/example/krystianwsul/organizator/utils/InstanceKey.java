package com.example.krystianwsul.organizator.utils;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;

import junit.framework.Assert;

public class InstanceKey implements Parcelable {
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

    public InstanceKey(int taskId, Date scheduleDate, TimePair scheduleTimePair) {
        Assert.assertTrue(scheduleDate != null);
        Assert.assertTrue(scheduleTimePair != null);

        TaskId = taskId;
        ScheduleDate = scheduleDate;
        ScheduleTimePair = scheduleTimePair;
    }

    @Override
    public int hashCode() {
        return TaskId + ScheduleDate.hashCode() + (ScheduleTimePair.CustomTimeId != null ? ScheduleTimePair.CustomTimeId : 0) + (ScheduleTimePair.HourMinute != null ? ScheduleTimePair.HourMinute.hashCode() : 0);
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

        if (ScheduleTimePair.CustomTimeId == null) {
            return (instanceKey.ScheduleTimePair.CustomTimeId == null && ScheduleTimePair.HourMinute.equals(instanceKey.ScheduleTimePair.HourMinute));
        } else {
            return (instanceKey.ScheduleTimePair.CustomTimeId != null && ScheduleTimePair.CustomTimeId.equals(instanceKey.ScheduleTimePair.CustomTimeId));
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

            Integer scheduleCustomTimeId = scheduleTimePair.CustomTimeId;
            HourMinute scheduleHourMinute = scheduleTimePair.HourMinute;
            Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

            return new InstanceKey(taskId, scheduleDate, scheduleCustomTimeId, scheduleHourMinute);
        }

        @Override
        public InstanceKey[] newArray(int size) {
            return new InstanceKey[0];
        }
    };
}
