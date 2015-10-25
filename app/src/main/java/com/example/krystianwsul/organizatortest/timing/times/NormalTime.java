package com.example.krystianwsul.organizatortest.timing.times;

import com.example.krystianwsul.organizatortest.timing.DayOfWeek;

/**
 * Created by Krystian on 10/13/2015.
 */
public class NormalTime implements com.example.krystianwsul.organizatortest.timing.times.Time {
    private HourMinute mTime;

    public NormalTime(HourMinute time) {
        mTime = time;
    }

    public String getName() {
        return mTime.getHour() + ":" + mTime.getMinute();
    }

    public HourMinute getTimeByDay(DayOfWeek day) {
        return mTime;
    }

    public String toString() {
        return mTime.toString();
    }
}
