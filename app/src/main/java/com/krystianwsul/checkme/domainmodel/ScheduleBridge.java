package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

import java.util.Set;

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

    void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends);
}
