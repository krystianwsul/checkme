package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.krystianwsul.checkme.persistencemodel.CustomTimeRecord;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.TreeMap;

public class CustomTime implements Time {
    @NonNull
    private final CustomTimeRecord mCustomTimeRecord;

    public CustomTime(@NonNull CustomTimeRecord customTimeRecord) {
        mCustomTimeRecord = customTimeRecord;
    }

    @NonNull
    public String getName() {
        return mCustomTimeRecord.getName();
    }

    void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        mCustomTimeRecord.setName(name);
    }

    @NonNull
    @Override
    public HourMinute getHourMinute(@NonNull DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case SUNDAY:
                return new HourMinute(mCustomTimeRecord.getSundayHour(), mCustomTimeRecord.getSundayMinute());
            case MONDAY:
                return new HourMinute(mCustomTimeRecord.getMondayHour(), mCustomTimeRecord.getMondayMinute());
            case TUESDAY:
                return new HourMinute(mCustomTimeRecord.getTuesdayHour(), mCustomTimeRecord.getTuesdayMinute());
            case WEDNESDAY:
                return new HourMinute(mCustomTimeRecord.getWednesdayHour(), mCustomTimeRecord.getWednesdayMinute());
            case THURSDAY:
                return new HourMinute(mCustomTimeRecord.getThursdayHour(), mCustomTimeRecord.getThursdayMinute());
            case FRIDAY:
                return new HourMinute(mCustomTimeRecord.getFridayHour(), mCustomTimeRecord.getFridayMinute());
            case SATURDAY:
                return new HourMinute(mCustomTimeRecord.getSaturdayHour(), mCustomTimeRecord.getSaturdayMinute());
            default:
                throw new IllegalArgumentException("invalid day: " + dayOfWeek);
        }
    }

    @NonNull
    TreeMap<DayOfWeek, HourMinute> getHourMinutes() {
        TreeMap<DayOfWeek, HourMinute> hourMinutes = new TreeMap<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values())
            hourMinutes.put(dayOfWeek, getHourMinute(dayOfWeek));
        return hourMinutes;
    }

    void setHourMinute(@NonNull DayOfWeek dayOfWeek, @NonNull HourMinute hourMinute) {
        switch (dayOfWeek) {
            case SUNDAY:
                mCustomTimeRecord.setSundayHour(hourMinute.getHour());
                mCustomTimeRecord.setSundayMinute(hourMinute.getMinute());
                break;
            case MONDAY:
                mCustomTimeRecord.setMondayHour(hourMinute.getHour());
                mCustomTimeRecord.setMondayMinute(hourMinute.getMinute());
                break;
            case TUESDAY:
                mCustomTimeRecord.setTuesdayHour(hourMinute.getHour());
                mCustomTimeRecord.setTuesdayMinute(hourMinute.getMinute());
                break;
            case WEDNESDAY:
                mCustomTimeRecord.setWednesdayHour(hourMinute.getHour());
                mCustomTimeRecord.setWednesdayMinute(hourMinute.getMinute());
                break;
            case THURSDAY:
                mCustomTimeRecord.setThursdayHour(hourMinute.getHour());
                mCustomTimeRecord.setThursdayMinute(hourMinute.getMinute());
                break;
            case FRIDAY:
                mCustomTimeRecord.setFridayHour(hourMinute.getHour());
                mCustomTimeRecord.setFridayMinute(hourMinute.getMinute());
                break;
            case SATURDAY:
                mCustomTimeRecord.setSaturdayHour(hourMinute.getHour());
                mCustomTimeRecord.setSaturdayMinute(hourMinute.getMinute());
                break;
            default:
                throw new IllegalArgumentException("invalid day: " + dayOfWeek);
        }
    }

    @NonNull
    public String toString() {
        return getName();
    }

    public int getId() {
        return mCustomTimeRecord.getId();
    }

    @NonNull
    @Override
    public Pair<CustomTime, HourMinute> getPair() {
        return new Pair<>(this, null);
    }

    boolean getCurrent() {
        return mCustomTimeRecord.getCurrent();
    }

    void setCurrent() {
        mCustomTimeRecord.setCurrent(false);
    }

    @NonNull
    @Override
    public TimePair getTimePair() {
        return new TimePair(mCustomTimeRecord.getId(), null);
    }

    void setRelevant() {
        mCustomTimeRecord.setRelevant(false);
    }
}
