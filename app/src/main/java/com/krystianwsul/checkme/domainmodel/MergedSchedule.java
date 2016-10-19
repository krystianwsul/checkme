package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

public interface MergedSchedule {
    boolean isVisible(@NonNull MergedTask task, @NonNull ExactTimeStamp now);

    @Nullable
    Integer getCustomTimeId();

    boolean current(@NonNull ExactTimeStamp exactTimeStamp);

    @NonNull
    ScheduleType getType();

    @Nullable
    TimeStamp getNextAlarm(@NonNull ExactTimeStamp now);
}
