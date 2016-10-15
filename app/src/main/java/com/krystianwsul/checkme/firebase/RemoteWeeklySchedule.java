package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

public class RemoteWeeklySchedule extends RemoteSchedule {
    private final int mPosition;

    @NonNull
    private RemoteWeeklyScheduleRecord mRemoteWeeklyScheduleRecord;

    public RemoteWeeklySchedule(int position, @NonNull RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord) {
        mPosition = position;
        mRemoteWeeklyScheduleRecord = remoteWeeklyScheduleRecord;
    }

    @NonNull
    @Override
    protected RemoteScheduleRecord getRemoteScheduleRecord() {
        return mRemoteWeeklyScheduleRecord;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        return getDayOfWeek() + ": " + getTime();
    }

    @NonNull
    private DayOfWeek getDayOfWeek() {
        DayOfWeek dayOfWeek = DayOfWeek.values()[mRemoteWeeklyScheduleRecord.getDayOfWeek()];
        Assert.assertTrue(dayOfWeek != null);

        return dayOfWeek;
    }

    @NonNull
    private Time getTime() {
        Assert.assertTrue(mRemoteWeeklyScheduleRecord.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mRemoteWeeklyScheduleRecord.getHour() != null);
        Assert.assertTrue(mRemoteWeeklyScheduleRecord.getMinute() != null);

        //Integer customTimeId = mWeeklyScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mWeeklyScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mRemoteWeeklyScheduleRecord.getHour();
        Integer minute = mRemoteWeeklyScheduleRecord.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @NonNull
    @Override
    public String getPath() {
        return "weeklyScheduleRecords/" + mPosition;
    }
}
