package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class CustomTimeRecord {
    private final int mId;
    private final String mName;

    private Integer mSundayHour;
    private Integer mSundayMinute;

    private Integer mMondayHour;
    private Integer mMondayMinute;

    private Integer mTuesdayHour;
    private Integer mTuesdayMinute;

    private Integer mWednesdayHour;
    private Integer mWednesdayMinute;

    private Integer mThursdayHour;
    private Integer mThursdayMinute;

    private Integer mFridayHour;
    private Integer mFridayMinute;

    private Integer mSaturdayHour;
    private Integer mSaturdayMinute;

    public CustomTimeRecord(int id, String name, Integer sundayHour, Integer sundayMinute, Integer mondayHour, Integer mondayMinute, Integer tuesdayHour, Integer tuesdayMinute, Integer wednesdayHour, Integer wednesdayMinute, Integer thursdayHour, Integer thursdayMinute, Integer fridayHour, Integer fridayMinute, Integer saturdayHour, Integer saturdayMinute) {
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

    public void setSundayHour(int hour) {
        mSundayHour = hour;
    }

    public void setSundayMinute(int minute) {
        mSundayMinute = minute;
    }

    public void setMondayHour(int hour) {
        mMondayHour = hour;
    }

    public void setMondayMinute(int minute) {
        mMondayMinute = minute;
    }

    public void setTuesdayHour(int hour) {
        mTuesdayHour = hour;
    }

    public void setTuesdayMinute(int minute) {
        mTuesdayMinute = minute;
    }

    public void setWednesdayHour(int hour) {
        mTuesdayHour = hour;
    }

    public void setWednesdayMinute(int minute) {
        mTuesdayMinute = minute;
    }

    public void setThursdayHour(int hour) {
        mThursdayHour = hour;
    }

    public void setThursdayMinute(int minute) {
        mThursdayMinute = minute;
    }

    public void setFridayHour(int hour) {
        mThursdayHour = hour;
    }

    public void setFridayMinute(int minute) {
        mThursdayMinute = minute;
    }

    public void setSaturdayHour(int hour) {
        mThursdayHour = hour;
    }

    public void setSaturdayMinute(int minute) {
        mThursdayMinute = minute;
    }
}
