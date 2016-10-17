package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

class RemoteMonthlyDaySchedule extends RemoteSchedule {
    @NonNull
    private final RemoteMonthlyDayScheduleRecord mRemoteMonthlyDayScheduleRecord;

    RemoteMonthlyDaySchedule(@NonNull RemoteMonthlyDayScheduleRecord remoteMonthlyDaySchedulRecord) {
        mRemoteMonthlyDayScheduleRecord = remoteMonthlyDaySchedulRecord;
    }

    @NonNull
    @Override
    protected RemoteScheduleRecord getRemoteScheduleRecord() {
        return mRemoteMonthlyDayScheduleRecord;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        String day = mRemoteMonthlyDayScheduleRecord.getDayOfMonth() + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mRemoteMonthlyDayScheduleRecord.getBeginningOfMonth() ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

        return day + ": " + getTime();
    }

    @NonNull
    private Time getTime() {
        Assert.assertTrue(mRemoteMonthlyDayScheduleRecord.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mRemoteMonthlyDayScheduleRecord.getHour() != null);
        Assert.assertTrue(mRemoteMonthlyDayScheduleRecord.getMinute() != null);

        //Integer customTimeId = mMonthlyDayScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mMonthlyDayScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mRemoteMonthlyDayScheduleRecord.getHour();
        Integer minute = mRemoteMonthlyDayScheduleRecord.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        mRemoteMonthlyDayScheduleRecord.setEndTime(now.getLong());
    }
}
