package com.example.krystianwsul.organizatortest.timing.times;

import com.example.krystianwsul.organizatortest.timing.DayOfWeek;

/**
 * Created by Krystian on 10/13/2015.
 */
public interface Time {
    String getName();
    HourMinute getTimeByDay(DayOfWeek day);
}
