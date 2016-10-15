package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.MergedSchedule;
import com.krystianwsul.checkme.domainmodel.MergedTask;
import com.krystianwsul.checkme.firebase.json.ScheduleJson;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

public abstract class RemoteSchedule implements MergedSchedule {
    public RemoteSchedule() {

    }

    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    abstract String getScheduleText(@NonNull Context context);

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
    protected abstract ScheduleJson getRemoteScheduleRecord();

    @Override
    public boolean isVisible(@NonNull MergedTask task, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        return true; // todo firebase
    }

    @NonNull
    public abstract String getPath();
}
