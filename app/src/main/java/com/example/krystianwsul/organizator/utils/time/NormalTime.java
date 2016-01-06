package com.example.krystianwsul.organizator.utils.time;

import android.support.v4.util.Pair;

import com.example.krystianwsul.organizator.domainmodel.CustomTime;

public class NormalTime implements Time {
    private final HourMinute mHourMinute;

    public static NormalTime getNow() {
        return new NormalTime(TimeStamp.getNow().getHourMinute());
    }

    public NormalTime(HourMinute hourMinute) {
        mHourMinute = hourMinute;
    }

    public NormalTime(int hour, int minute) {
        mHourMinute = new HourMinute(hour, minute);
    }

    public String getName() {
        return mHourMinute.getHour() + ":" + mHourMinute.getMinute();
    }

    public HourMinute getHourMinute(DayOfWeek dayOfWeek) {
        return mHourMinute;
    }

    public HourMinute getHourMinute() {
        return mHourMinute;
    }

    public String toString() {
        return mHourMinute.toString();
    }

    public Pair<CustomTime, HourMinute> getPair() {
        return new Pair<>(null, mHourMinute);
    }
}
