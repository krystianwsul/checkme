package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class TimeRecord {
    private final int mId;
    private final String mName;

    private final Integer mSundayHour;
    private final Integer mSundayMinute;

    private final Integer mMondayHour;
    private final Integer mMondayMinute;

    private final Integer mTuesdayHour;
    private final Integer mTuesdayMinute;

    private final Integer mWednesdayHour;
    private final Integer mWednesdayMinute;

    private final Integer mThursdayHour;
    private final Integer mThursdayMinute;

    private final Integer mFridayHour;
    private final Integer mFridayMinute;

    private final Integer mSaturdayHour;
    private final Integer mSaturdayMinute;

    public TimeRecord(int id, String name, Integer sundayHour, Integer sundayMinute, Integer mondayHour, Integer mondayMinute, Integer tuesdayHour, Integer tuesdayMinute, Integer wednesdayHour, Integer wednesdayMinute, Integer thursdayHour, Integer thursdayMinute, Integer fridayHour, Integer fridayMinute, Integer saturdayHour, Integer saturdayMinute) {
        Assert.assertTrue(name != null);
        Assert.assertTrue((sundayHour == null) == (sundayMinute == null));
        Assert.assertTrue((mondayHour == null) == (mondayMinute == null));
        Assert.assertTrue((tuesdayHour == null) == (tuesdayMinute == null));
        Assert.assertTrue((wednesdayHour == null) == (wednesdayMinute == null));
        Assert.assertTrue((thursdayHour == null) == (thursdayMinute == null));
        Assert.assertTrue((fridayHour == null) == (fridayMinute == null));
        Assert.assertTrue((saturdayHour == null) == (saturdayMinute == null));
        Assert.assertTrue((sundayHour != null) || (mondayHour != null) || (tuesdayHour != null) || (wednesdayHour != null) || (thursdayHour != null) || (fridayHour != null) || (saturdayHour != null));

        mId = id;
        mName = name;

        mSundayHour = sundayHour;
        mSundayMinute = sundayMinute;

        mMondayHour = mondayHour;
        mMondayMinute = mondayMinute;

        mTuesdayHour = tuesdayHour;
        mTuesdayMinute = tuesdayMinute;

        mWednesdayHour = wednesdayHour;
        mWednesdayMinute = wednesdayMinute;

        mThursdayHour = thursdayHour;
        mThursdayMinute = thursdayMinute;

        mFridayHour = fridayHour;
        mFridayMinute = fridayMinute;

        mSaturdayHour = saturdayHour;
        mSaturdayMinute = saturdayMinute;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public Integer getSundayHour() {
        return mSundayHour;
    }

    public Integer getSundayMinute() {
        return mSundayMinute;
    }

    public Integer getMondayHour() {
        return mMondayHour;
    }

    public Integer getMondayMinute() {
        return mMondayMinute;
    }

    public Integer getTuesdayHour() {
        return mTuesdayHour;
    }

    public Integer getTuesdayMinute() {
        return mTuesdayMinute;
    }

    public Integer getWednesdayHour() {
        return mWednesdayHour;
    }

    public Integer getWednesdayMinute() {
        return mWednesdayMinute;
    }

    public Integer getThursdayHour() {
        return mThursdayHour;
    }

    public Integer getThursdayMinute() {
        return mThursdayMinute;
    }

    public Integer getFridayHour() {
        return mFridayHour;
    }

    public Integer getFridayMinute() {
        return mFridayMinute;
    }

    public Integer getSaturdayHour() {
        return mSaturdayHour;
    }

    public Integer getSaturdayMinute() {
        return mSaturdayMinute;
    }
}
