package com.krystianwsul.checkme.utils.time;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.krystianwsul.checkme.domainmodel.CustomTime;

public interface Time {
    HourMinute getHourMinute(DayOfWeek dayOfWeek);

    @NonNull
    Pair<CustomTime, HourMinute> getPair();

    @NonNull
    TimePair getTimePair();
}
