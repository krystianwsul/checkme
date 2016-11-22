package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

public interface ScheduleBridge {
    long getStartTime();

    @Nullable
    Long getEndTime();

    void setEndTime(long endTime);

    @NonNull
    TaskKey getRootTaskKey();

    @NonNull
    ScheduleType getScheduleType();

    void delete();
}
