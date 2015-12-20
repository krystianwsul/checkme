package com.example.krystianwsul.organizator.domainmodel.times;

import android.support.v4.util.Pair;

import com.example.krystianwsul.organizator.domainmodel.dates.DayOfWeek;

public interface Time {
    String getName();
    HourMinute getHourMinute(DayOfWeek dayOfWeek);
    Pair<CustomTime, HourMinute> getPair();
}
