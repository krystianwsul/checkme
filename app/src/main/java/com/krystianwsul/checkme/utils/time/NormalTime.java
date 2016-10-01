package com.krystianwsul.checkme.utils.time;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.krystianwsul.checkme.domainmodel.CustomTime;

public class NormalTime implements Time {
    private final HourMinute mHourMinute;

    public NormalTime(HourMinute hourMinute) {
        mHourMinute = hourMinute;
    }

    public NormalTime(int hour, int minute) {
        mHourMinute = new HourMinute(hour, minute);
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

    @NonNull
    @Override
    public Pair<CustomTime, HourMinute> getPair() {
        return new Pair<>(null, mHourMinute);
    }

    @NonNull
    @Override
    public TimePair getTimePair() {
        return new TimePair(null, mHourMinute);
    }
}
