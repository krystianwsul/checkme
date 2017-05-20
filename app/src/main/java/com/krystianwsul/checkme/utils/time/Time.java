package com.krystianwsul.checkme.utils.time;

import android.support.annotation.NonNull;

public interface Time {
    @NonNull
    HourMinute getHourMinute(@NonNull DayOfWeek dayOfWeek);

    @NonNull
    TimePair getTimePair();

    @NonNull
    String toString();
}
