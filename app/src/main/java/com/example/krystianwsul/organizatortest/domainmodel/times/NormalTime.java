package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;

import java.security.Timestamp;

public class NormalTime implements com.example.krystianwsul.organizatortest.domainmodel.times.Time {
    private HourMinute mTime;

    public static NormalTime getNow() {
        return new NormalTime(TimeStamp.getNow().getHourMinute());
    }

    public NormalTime(HourMinute time) {
        mTime = time;
    }

    public NormalTime(int hour, int minute) {
        mTime = new HourMinute(hour, minute);
    }

    public String getName() {
        return mTime.getHour() + ":" + mTime.getMinute();
    }

    public HourMinute getHourMinute(DayOfWeek dayOfWeek) {
        return mTime;
    }

    public String toString() {
        return mTime.toString();
    }
}
