package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.MergedSchedule;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

abstract class RemoteSchedule implements MergedSchedule {
    RemoteSchedule() {

    }

    @Override
    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(getRemoteScheduleRecord().getStartTime());
    }

    @Nullable
    ExactTimeStamp getEndExactTimeStamp() {
        if (getRemoteScheduleRecord().getEndTime() == null)
            return null;
        else
            return new ExactTimeStamp(getRemoteScheduleRecord().getEndTime());
    }

    @NonNull
    protected abstract RemoteScheduleRecord getRemoteScheduleRecord();

    @Override
    public boolean isVisible(@NonNull Task task, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        return true; // todo firebase
    }

    @NonNull
    public String getTaskId() {
        return getRemoteScheduleRecord().getTaskId();
    }
}
