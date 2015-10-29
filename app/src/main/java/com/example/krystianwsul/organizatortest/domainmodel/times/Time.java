package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;

/**
 * Created by Krystian on 10/13/2015.
 */
public interface Time {
    String getName();
    HourMinute getTimeByDay(DayOfWeek day);
}
