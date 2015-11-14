package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/27/2015.
 */
public class CustomTimeRecord {
    private final int mId;
    private final String mName;

    private int mSundayHour;
    private int mSundayMinute;

    private int mMondayHour;
    private int mMondayMinute;

    private int mTuesdayHour;
    private int mTuesdayMinute;

    private int mWednesdayHour;
    private int mWednesdayMinute;

    private int mThursdayHour;
    private int mThursdayMinute;

    private int mFridayHour;
    private int mFridayMinute;

    private int mSaturdayHour;
    private int mSaturdayMinute;

    CustomTimeRecord(int id, String name, int sundayHour, int sundayMinute, int mondayHour, int mondayMinute, int tuesdayHour, int tuesdayMinute, int wednesdayHour, int wednesdayMinute, int thursdayHour, int thursdayMinute, int fridayHour, int fridayMinute, int saturdayHour, int saturdayMinute) {
        Assert.assertTrue(name != null);

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

    public int getSundayHour() {
        return mSundayHour;
    }

    public int getSundayMinute() {
        return mSundayMinute;
    }

    public int getMondayHour() {
        return mMondayHour;
    }

    public int getMondayMinute() {
        return mMondayMinute;
    }

    public int getTuesdayHour() {
        return mTuesdayHour;
    }

    public int getTuesdayMinute() {
        return mTuesdayMinute;
    }

    public int getWednesdayHour() {
        return mWednesdayHour;
    }

    public int getWednesdayMinute() {
        return mWednesdayMinute;
    }

    public int getThursdayHour() {
        return mThursdayHour;
    }

    public int getThursdayMinute() {
        return mThursdayMinute;
    }

    public int getFridayHour() {
        return mFridayHour;
    }

    public int getFridayMinute() {
        return mFridayMinute;
    }

    public int getSaturdayHour() {
        return mSaturdayHour;
    }

    public int getSaturdayMinute() {
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
        mWednesdayHour = hour;
    }

    public void setWednesdayMinute(int minute) {
        mWednesdayMinute = minute;
    }

    public void setThursdayHour(int hour) {
        mThursdayHour = hour;
    }

    public void setThursdayMinute(int minute) {
        mThursdayMinute = minute;
    }

    public void setFridayHour(int hour) {
        mFridayHour = hour;
    }

    public void setFridayMinute(int minute) {
        mFridayMinute = minute;
    }

    public void setSaturdayHour(int hour) {
        mSaturdayHour = hour;
    }

    public void setSaturdayMinute(int minute) {
        mSaturdayMinute = minute;
    }
}
