package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import junit.framework.Assert;

public class CustomTimeJson {
    private String ownerId;
    private int localId;

    private String name;

    private int sundayHour;
    private int sundayMinute;

    private int mondayHour;
    private int mondayMinute;

    private int tuesdayHour;
    private int tuesdayMinute;

    private int wednesdayHour;
    private int wednesdayMinute;

    private int thursdayHour;
    private int thursdayMinute;

    private int fridayHour;
    private int fridayMinute;

    private int saturdayHour;
    private int saturdayMinute;

    public CustomTimeJson() {

    }

    CustomTimeJson(@NonNull String ownerId, int localId, String name, int sundayHour, int sundayMinute, int mondayHour, int mondayMinute, int tuesdayHour, int tuesdayMinute, int wednesdayHour, int wednesdayMinute, int thursdayHour, int thursdayMinute, int fridayHour, int fridayMinute, int saturdayHour, int saturdayMinute) {
        Assert.assertTrue(!TextUtils.isEmpty(ownerId));
        Assert.assertTrue(!TextUtils.isEmpty(name));

        this.ownerId = ownerId;
        this.localId = localId;

        this.name = name;

        this.sundayHour = sundayHour;
        this.sundayMinute = sundayMinute;

        this.mondayHour = mondayHour;
        this.mondayMinute = mondayMinute;

        this.tuesdayHour = tuesdayHour;
        this.tuesdayMinute = tuesdayMinute;

        this.wednesdayHour = wednesdayHour;
        this.wednesdayMinute = wednesdayMinute;

        this.thursdayHour = thursdayHour;
        this.thursdayMinute = thursdayMinute;

        this.fridayHour = fridayHour;
        this.fridayMinute = fridayMinute;

        this.saturdayHour = saturdayHour;
        this.saturdayMinute = saturdayMinute;
    }

    @NonNull
    public String getOwnerId() {
        Assert.assertTrue(!TextUtils.isEmpty(ownerId));

        return ownerId;
    }

    public int getLocalId() {
        return localId;
    }

    @NonNull
    public String getName() {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        return name;
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        this.name = name;
    }

    public int getSundayHour() {
        return sundayHour;
    }

    public int getSundayMinute() {
        return sundayMinute;
    }

    public int getMondayHour() {
        return mondayHour;
    }

    public int getMondayMinute() {
        return mondayMinute;
    }

    public int getTuesdayHour() {
        return tuesdayHour;
    }

    public int getTuesdayMinute() {
        return tuesdayMinute;
    }

    public int getWednesdayHour() {
        return wednesdayHour;
    }

    public int getWednesdayMinute() {
        return wednesdayMinute;
    }

    public int getThursdayHour() {
        return thursdayHour;
    }

    public int getThursdayMinute() {
        return thursdayMinute;
    }

    public int getFridayHour() {
        return fridayHour;
    }

    public int getFridayMinute() {
        return fridayMinute;
    }

    public int getSaturdayHour() {
        return saturdayHour;
    }

    public int getSaturdayMinute() {
        return saturdayMinute;
    }

    public void setSundayHour(int hour) {
        sundayHour = hour;
    }

    public void setSundayMinute(int minute) {
        sundayMinute = minute;
    }

    public void setMondayHour(int hour) {
        mondayHour = hour;
    }

    public void setMondayMinute(int minute) {
        mondayMinute = minute;
    }

    public void setTuesdayHour(int hour) {
        tuesdayHour = hour;
    }

    public void setTuesdayMinute(int minute) {
        tuesdayMinute = minute;
    }

    public void setWednesdayHour(int hour) {
        wednesdayHour = hour;
    }

    public void setWednesdayMinute(int minute) {
        wednesdayMinute = minute;
    }

    public void setThursdayHour(int hour) {
        thursdayHour = hour;
    }

    public void setThursdayMinute(int minute) {
        thursdayMinute = minute;
    }

    public void setFridayHour(int hour) {
        fridayHour = hour;
    }

    public void setFridayMinute(int minute) {
        fridayMinute = minute;
    }

    public void setSaturdayHour(int hour) {
        saturdayHour = hour;
    }

    public void setSaturdayMinute(int minute) {
        saturdayMinute = minute;
    }
}
