package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

class RemoteDailySchedule extends RemoteSchedule {
    @NonNull
    private final RemoteDailyScheduleRecord mRemoteDailyScheduleRecord;

    RemoteDailySchedule(@NonNull RemoteDailyScheduleRecord remoteDailyScheduleRecord) {
        mRemoteDailyScheduleRecord = remoteDailyScheduleRecord;
    }

    @NonNull
    @Override
    protected RemoteScheduleRecord getRemoteScheduleRecord() {
        return mRemoteDailyScheduleRecord;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        return context.getString(R.string.daily) + " " + getTime().toString();
    }

    @NonNull
    Time getTime() {
        Assert.assertTrue(mRemoteDailyScheduleRecord.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mRemoteDailyScheduleRecord.getHour() != null);
        Assert.assertTrue(mRemoteDailyScheduleRecord.getMinute() != null);

        //Integer customTimeId = mDailyScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mDailyScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mRemoteDailyScheduleRecord.getHour();
        Integer minute = mRemoteDailyScheduleRecord.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        mRemoteDailyScheduleRecord.setEndTime(now.getLong());
    }
}
