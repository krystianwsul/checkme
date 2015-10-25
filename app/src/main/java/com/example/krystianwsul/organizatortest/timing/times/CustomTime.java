package com.example.krystianwsul.organizatortest.timing.times;

import java.util.HashMap;
import com.example.krystianwsul.organizatortest.timing.DayOfWeek;

/**
 * Created by Krystian on 10/12/2015.
 */
public class CustomTime implements Time {
    private String mName;
    private HashMap<DayOfWeek, HourMinute> mTimes;

    public CustomTime(String name, HourMinute monday, HourMinute tuesday, HourMinute wednesday, HourMinute thursday, HourMinute friday, HourMinute saturday, HourMinute sunday) {
        mName = name;

        mTimes = new HashMap<>(7);

        mTimes.put(DayOfWeek.MONDAY, monday);
        mTimes.put(DayOfWeek.TUESDAY, tuesday);
        mTimes.put(DayOfWeek.WEDNESDAY, wednesday);
        mTimes.put(DayOfWeek.THURSDAY, thursday);
        mTimes.put(DayOfWeek.FRIDAY, friday);
        mTimes.put(DayOfWeek.SATURDAY, saturday);
        mTimes.put(DayOfWeek.SUNDAY, sunday);
    }

    public String getName() {
        return mName;
    }

    public HourMinute getTimeByDay(DayOfWeek day) {
        return mTimes.get(day);
    }

    public String toString() {
        return getName();
    }
}
