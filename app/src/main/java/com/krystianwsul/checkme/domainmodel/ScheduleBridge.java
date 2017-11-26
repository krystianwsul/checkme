package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.krystianwsul.checkme.utils.TaskKey;

public interface ScheduleBridge {
    long getStartTime();

    @Nullable
    Long getEndTime();

    void setEndTime(long endTime);

    @NonNull
    TaskKey getRootTaskKey();

    void delete();

    @Nullable
    Pair<String, String> getRemoteCustomTimeKey();
}
