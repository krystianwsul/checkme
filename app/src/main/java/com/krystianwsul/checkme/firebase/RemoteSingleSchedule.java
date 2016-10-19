package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedInstance;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteSingleScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

class RemoteSingleSchedule extends RemoteSchedule {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteSingleScheduleRecord mRemoteSingleScheduleRecord;

    RemoteSingleSchedule(@NonNull DomainFactory domainFactory, @NonNull RemoteSingleScheduleRecord remoteSingleScheduleRecord) {
        mDomainFactory = domainFactory;
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

    @NonNull
    @Override
    List<MergedInstance> getInstances(@NonNull RemoteTask task, ExactTimeStamp givenStartExactTimeStamp, @NonNull ExactTimeStamp givenExactEndTimeStamp) {
        List<MergedInstance> instances = new ArrayList<>();

        ExactTimeStamp singleScheduleExactTimeStamp = getDateTime().getTimeStamp().toExactTimeStamp();

        if (givenStartExactTimeStamp != null && givenStartExactTimeStamp.compareTo(singleScheduleExactTimeStamp) > 0) {
            return instances;
        }

        if (givenExactEndTimeStamp.compareTo(singleScheduleExactTimeStamp) <= 0) {
            return instances;
        }

        instances.add(getInstance(task));

        return instances;
    }

    @NonNull
    MergedInstance getInstance(@NonNull RemoteTask task) {
        return mDomainFactory.getInstance(task, getDateTime());
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mRemoteSingleScheduleRecord.getCustomTimeId();
    }

    @NonNull
    @Override
    public ScheduleType getType() {
        return ScheduleType.SINGLE;
    }

    @Nullable
    @Override
    public TimeStamp getNextAlarm(@NonNull ExactTimeStamp now) {
        TimeStamp timeStamp = getDateTime().getTimeStamp();
        if (timeStamp.toExactTimeStamp().compareTo(now) > 0)
            return timeStamp;
        else
            return null;
    }
}
