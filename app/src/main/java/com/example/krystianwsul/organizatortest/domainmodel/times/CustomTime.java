package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.CustomTimeRecord;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;

import junit.framework.Assert;

import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Krystian on 10/12/2015.
 */
public class CustomTime implements Time {
    private final CustomTimeRecord mCustomTimeRecord;

    private static HashMap<Integer, CustomTime> sCustomTimes = new HashMap<>();

    public static CustomTime getCustomTime(int customTimeId) {
        if (sCustomTimes.containsKey(customTimeId)) {
            return sCustomTimes.get(customTimeId);
        } else {
            CustomTime customTime = new CustomTime(customTimeId);
            sCustomTimes.put(customTimeId, customTime);
            return customTime;
        }
    }

    public static CustomTime getCustomTime(DayOfWeek dayOfWeek, HourMinute hourMinute) {
        for (CustomTime customTime : sCustomTimes.values())
            if (customTime.getHourMinute(dayOfWeek).compareTo(hourMinute) == 0)
                return customTime;
        return null;
    }

    public static Collection<CustomTime> getCustomTimes() {
        return sCustomTimes.values();
    }

    private CustomTime(int customTimeId) {
        mCustomTimeRecord = PersistenceManger.getInstance().getCustomTimeRecord(customTimeId);
        Assert.assertTrue(mCustomTimeRecord != null);
    }

    public String getName() {
        return mCustomTimeRecord.getName();
    }

    public HourMinute getHourMinute(DayOfWeek dayOfWeek) {
        Assert.assertTrue(dayOfWeek != null);

        switch (dayOfWeek) {
            case SUNDAY:
                if (mCustomTimeRecord.getSundayHour() != null)
                    return new HourMinute(mCustomTimeRecord.getSundayHour(), mCustomTimeRecord.getSundayMinute());
                else
                    return null;
            case MONDAY:
                if (mCustomTimeRecord.getMondayHour() != null)
                    return new HourMinute(mCustomTimeRecord.getMondayHour(), mCustomTimeRecord.getMondayMinute());
                else
                    return null;
            case TUESDAY:
                if (mCustomTimeRecord.getTuesdayHour() != null)
                    return new HourMinute(mCustomTimeRecord.getTuesdayHour(), mCustomTimeRecord.getTuesdayMinute());
                else
                    return null;
            case WEDNESDAY:
                if (mCustomTimeRecord.getWednesdayHour() != null)
                    return new HourMinute(mCustomTimeRecord.getWednesdayHour(), mCustomTimeRecord.getWednesdayMinute());
                else
                    return null;
            case THURSDAY:
                if (mCustomTimeRecord.getThursdayHour() != null)
                    return new HourMinute(mCustomTimeRecord.getThursdayHour(), mCustomTimeRecord.getThursdayMinute());
                else
                    return null;
            case FRIDAY:
                if (mCustomTimeRecord.getFridayHour() != null)
                    return new HourMinute(mCustomTimeRecord.getFridayHour(), mCustomTimeRecord.getFridayMinute());
                else
                    return null;
            case SATURDAY:
                if (mCustomTimeRecord.getSaturdayHour() != null)
                    return new HourMinute(mCustomTimeRecord.getSaturdayHour(), mCustomTimeRecord.getSaturdayMinute());
                else
                    return null;
            default:
                throw new IllegalArgumentException("invalid day: " + dayOfWeek);
        }
    }

    public void setHourMinute(DayOfWeek dayOfWeek, HourMinute hourMinute) {
        Assert.assertTrue(dayOfWeek != null);
        Assert.assertTrue(hourMinute != null);

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

    public String toString() {
        return getName();
    }

    public int getId() {
        return mCustomTimeRecord.getId();
    }
}
