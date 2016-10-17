package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteSingleScheduleRecord;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

class RemoteSingleSchedule extends RemoteSchedule {
    @NonNull
    private final RemoteSingleScheduleRecord mRemoteSingleScheduleRecord;

    RemoteSingleSchedule(@NonNull RemoteSingleScheduleRecord remoteSingleScheduleRecord) {
        mRemoteSingleScheduleRecord = remoteSingleScheduleRecord;
    }

    @NonNull
    @Override
    protected RemoteScheduleRecord getRemoteScheduleRecord() {
        return mRemoteSingleScheduleRecord;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        return getDateTime().getDisplayText(context);
    }

    @NonNull
    private DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    @NonNull
    private Time getTime() {
        Assert.assertTrue(mRemoteSingleScheduleRecord.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mRemoteSingleScheduleRecord.getHour() != null);
        Assert.assertTrue(mRemoteSingleScheduleRecord.getMinute() != null);

        //Integer customTimeId = mRemoteSingleScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //return mDomainFactory.getCustomTime(mRemoteSingleScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mRemoteSingleScheduleRecord.getHour();
        Integer minute = mRemoteSingleScheduleRecord.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @NonNull
    private Date getDate() {
        return new Date(mRemoteSingleScheduleRecord.getYear(), mRemoteSingleScheduleRecord.getMonth(), mRemoteSingleScheduleRecord.getDay());
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        mRemoteSingleScheduleRecord.setEndTime(now.getLong());
    }
}
