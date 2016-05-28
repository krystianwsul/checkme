package com.krystianwsul.checkme.utils.time;

import android.support.v4.util.Pair;

import com.krystianwsul.checkme.domainmodel.CustomTime;

public interface Time {
    HourMinute getHourMinute(DayOfWeek dayOfWeek);
    Pair<CustomTime, HourMinute> getPair();
    TimePair getTimePair();
}
