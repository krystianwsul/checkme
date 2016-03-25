package com.example.krystianwsul.organizator.utils.time;

import android.support.v4.util.Pair;

import com.example.krystianwsul.organizator.domainmodel.CustomTime;

public interface Time {
    HourMinute getHourMinute(DayOfWeek dayOfWeek);
    Pair<CustomTime, HourMinute> getPair();
    TimePair getTimePair();
}
