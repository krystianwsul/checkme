package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;

/**
 * Created by Krystian on 10/13/2015.
 */
public class NormalTime implements com.example.krystianwsul.organizatortest.domainmodel.times.Time {
    private HourMinute mTime;

    public NormalTime(HourMinute time) {
        mTime = time;
    }

    public NormalTime(int hour, int minute) {
        mTime = new HourMinute(hour, minute);
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
