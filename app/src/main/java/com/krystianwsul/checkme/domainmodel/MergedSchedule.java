package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import java.util.List;

public interface MergedSchedule {
    boolean isVisible(@NonNull Task task, @NonNull ExactTimeStamp now);

    @Nullable
    Integer getCustomTimeId();

    boolean current(@NonNull ExactTimeStamp exactTimeStamp);

    @NonNull
    ScheduleType getType();

    @Nullable
    TimeStamp getNextAlarm(@NonNull ExactTimeStamp now);

    @NonNull
    String getScheduleText(@NonNull Context context);

    void setEndExactTimeStamp(@NonNull ExactTimeStamp endExactTimeStamp);

    @NonNull
    List<MergedInstance> getInstances(@NonNull Task task, @Nullable ExactTimeStamp givenStartExactTimeStamp, @NonNull ExactTimeStamp givenExactEndTimeStamp);
}
