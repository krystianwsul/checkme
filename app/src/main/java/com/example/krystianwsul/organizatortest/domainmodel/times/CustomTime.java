package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.CustomTimeRecord;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Krystian on 10/12/2015.
 */
public class CustomTime implements Time {
    private final CustomTimeRecord mCustomTimeRecord;

    private final ArrayList<CustomTimeListener> mCustomTimeListeners = new ArrayList<>();

    CustomTime(int customTimeId) {
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

    public void setHourMinute(DayOfWeek dayOfWeek, HourMinute newHourMinute) {
        Assert.assertTrue(dayOfWeek != null);
        Assert.assertTrue(newHourMinute != null);

        HourMinute oldHourMinute = getHourMinute(dayOfWeek);

        switch (dayOfWeek) {
            case SUNDAY:
                mCustomTimeRecord.setSundayHour(newHourMinute.getHour());
                mCustomTimeRecord.setSundayMinute(newHourMinute.getMinute());
                break;
            case MONDAY:
                mCustomTimeRecord.setMondayHour(newHourMinute.getHour());
                mCustomTimeRecord.setMondayMinute(newHourMinute.getMinute());
                break;
            case TUESDAY:
                mCustomTimeRecord.setTuesdayHour(newHourMinute.getHour());
                mCustomTimeRecord.setTuesdayMinute(newHourMinute.getMinute());
                break;
            case WEDNESDAY:
                mCustomTimeRecord.setWednesdayHour(newHourMinute.getHour());
                mCustomTimeRecord.setWednesdayMinute(newHourMinute.getMinute());
                break;
            case THURSDAY:
                mCustomTimeRecord.setThursdayHour(newHourMinute.getHour());
                mCustomTimeRecord.setThursdayMinute(newHourMinute.getMinute());
                break;
            case FRIDAY:
                mCustomTimeRecord.setFridayHour(newHourMinute.getHour());
                mCustomTimeRecord.setFridayMinute(newHourMinute.getMinute());
                break;
            case SATURDAY:
                mCustomTimeRecord.setSaturdayHour(newHourMinute.getHour());
                mCustomTimeRecord.setSaturdayMinute(newHourMinute.getMinute());
                break;
            default:
                throw new IllegalArgumentException("invalid day: " + dayOfWeek);
        }

        notifyCustomTimeHourMinuteChange(dayOfWeek, oldHourMinute, newHourMinute);
    }

    public String toString() {
        return getName();
    }

    public int getId() {
        return mCustomTimeRecord.getId();
    }

    public interface CustomTimeListener {
        void onCustomTimeHourMinuteChange(CustomTime customTime, DayOfWeek dayOfWeek, HourMinute oldHourMinute, HourMinute newHourMinute);
    }

    public void addCustomTimeListener(CustomTimeListener customTimeListener) {
        Assert.assertTrue(customTimeListener != null);
        if (!mCustomTimeListeners.contains(customTimeListener))
            mCustomTimeListeners.add(customTimeListener);
    }

    public void removeCustomTimeListener(CustomTimeListener customTimeListener) {
        Assert.assertTrue(customTimeListener != null);
        Assert.assertTrue(mCustomTimeListeners.contains(customTimeListener));
        mCustomTimeListeners.remove(customTimeListener);
    }

    private void notifyCustomTimeHourMinuteChange(DayOfWeek dayOfWeek, HourMinute oldHourMinute, HourMinute newHourMinute) {
        Assert.assertTrue(dayOfWeek != null);
        Assert.assertTrue(oldHourMinute != null);
        Assert.assertTrue(newHourMinute != null);

        for (CustomTimeListener customTimeListener : mCustomTimeListeners)
            customTimeListener.onCustomTimeHourMinuteChange(this, dayOfWeek, oldHourMinute, newHourMinute);
    }
}
