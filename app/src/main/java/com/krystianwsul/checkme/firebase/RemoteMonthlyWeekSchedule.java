package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyWeekScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

class RemoteMonthlyWeekSchedule extends RemoteSchedule {
    @NonNull
    private final RemoteMonthlyWeekScheduleRecord mRemoteMonthlyWeekScheduleRecord;

    RemoteMonthlyWeekSchedule(@NonNull RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord) {
        mRemoteMonthlyWeekScheduleRecord = remoteMonthlyWeekScheduleRecord;
    }

    @NonNull
    @Override
    protected RemoteScheduleRecord getRemoteScheduleRecord() {
        return mRemoteMonthlyWeekScheduleRecord;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        String day = mRemoteMonthlyWeekScheduleRecord.getDayOfMonth() + " " + getDayOfWeek() + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mRemoteMonthlyWeekScheduleRecord.getBeginningOfMonth() ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

        return day + ": " + getTime();
    }

    @NonNull
    private DayOfWeek getDayOfWeek() {
        DayOfWeek dayOfWeek = DayOfWeek.values()[mRemoteMonthlyWeekScheduleRecord.getDayOfWeek()];
        Assert.assertTrue(dayOfWeek != null);

        return dayOfWeek;
    }

    @NonNull
    private Time getTime() {
        Assert.assertTrue(mRemoteMonthlyWeekScheduleRecord.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mRemoteMonthlyWeekScheduleRecord.getHour() != null);
        Assert.assertTrue(mRemoteMonthlyWeekScheduleRecord.getMinute() != null);

        //Integer customTimeId = mMonthlyDayScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mMonthlyDayScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mRemoteMonthlyWeekScheduleRecord.getHour();
        Integer minute = mRemoteMonthlyWeekScheduleRecord.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        mRemoteMonthlyWeekScheduleRecord.setEndTime(now.getLong());
    }
}
