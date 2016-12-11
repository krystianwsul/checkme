package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.CustomTimeJson;

import junit.framework.Assert;

public class NewRemoteCustomTimeRecord extends RemoteRecord {
    public static final String CUSTOM_TIMES = "customTimes";

    @NonNull
    private final String mId;

    @NonNull
    private final RemoteProjectRecord mRemoteProjectRecord;

    @NonNull
    private final CustomTimeJson mCustomTimeJson;

    NewRemoteCustomTimeRecord(@NonNull String id, @NonNull RemoteProjectRecord remoteProjectRecord, @NonNull CustomTimeJson customTimeJson) {
        super(false);

        mId = id;
        mRemoteProjectRecord = remoteProjectRecord;
        mCustomTimeJson = customTimeJson;
    }

    NewRemoteCustomTimeRecord(@NonNull RemoteProjectRecord remoteProjectRecord, @NonNull CustomTimeJson customTimeJson) {
        super(true);

        mId = DatabaseWrapper.getCustomTimeRecordId(remoteProjectRecord.getId());
        mRemoteProjectRecord = remoteProjectRecord;
        mCustomTimeJson = customTimeJson;
    }

    @NonNull
    @Override
    protected CustomTimeJson getCreateObject() {
        return mCustomTimeJson;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    @Override
    protected String getKey() {
        return mRemoteProjectRecord.getKey() + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + CUSTOM_TIMES + "/" + mId;
    }

    @NonNull
    public String getOwnerId() {
        return mCustomTimeJson.getOwnerId();
    }

    public int getLocalId() {
        return mCustomTimeJson.getLocalId();
    }

    @NonNull
    public String getName() {
        return mCustomTimeJson.getName();
    }

    public int getSundayHour() {
        return mCustomTimeJson.getSundayHour();
    }

    public int getSundayMinute() {
        return mCustomTimeJson.getSundayMinute();
    }

    public int getMondayHour() {
        return mCustomTimeJson.getMondayHour();
    }

    public int getMondayMinute() {
        return mCustomTimeJson.getMondayMinute();
    }

    public int getTuesdayHour() {
        return mCustomTimeJson.getTuesdayHour();
    }

    public int getTuesdayMinute() {
        return mCustomTimeJson.getTuesdayMinute();
    }

    public int getWednesdayHour() {
        return mCustomTimeJson.getWednesdayHour();
    }

    public int getWednesdayMinute() {
        return mCustomTimeJson.getWednesdayMinute();
    }

    public int getThursdayHour() {
        return mCustomTimeJson.getThursdayHour();
    }

    public int getThursdayMinute() {
        return mCustomTimeJson.getThursdayMinute();
    }

    public int getFridayHour() {
        return mCustomTimeJson.getFridayHour();
    }

    public int getFridayMinute() {
        return mCustomTimeJson.getFridayMinute();
    }

    public int getSaturdayHour() {
        return mCustomTimeJson.getSaturdayHour();
    }

    public int getSaturdayMinute() {
        return mCustomTimeJson.getSaturdayMinute();
    }

    @NonNull
    public String getProjectId() {
        return mRemoteProjectRecord.getId();
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (getName().equals(name))
            return;

        mCustomTimeJson.setName(name);
        addValue(getKey() + "/name", name);
    }

    public void setSundayHour(int hour) {
        if (getSundayHour() == hour)
            return;

        mCustomTimeJson.setSundayHour(hour);
        addValue(getKey() + "/sundayHour", hour);
    }

    public void setSundayMinute(int minute) {
        if (getSundayMinute() == minute)
            return;

        mCustomTimeJson.setSundayMinute(minute);
        addValue(getKey() + "/sundayMinute", minute);
    }

    public void setMondayHour(int hour) {
        if (getMondayHour() == hour)
            return;

        mCustomTimeJson.setMondayHour(hour);
        addValue(getKey() + "/mondayHour", hour);
    }

    public void setMondayMinute(int minute) {
        if (getMondayMinute() == minute)
            return;

        mCustomTimeJson.setMondayMinute(minute);
        addValue(getKey() + "/mondayMinute", minute);
    }

    public void setTuesdayHour(int hour) {
        if (getTuesdayHour() == hour)
            return;

        mCustomTimeJson.setTuesdayHour(hour);
        addValue(getKey() + "/tuesdayHour", hour);
    }

    public void setTuesdayMinute(int minute) {
        if (getTuesdayMinute() == minute)
            return;

        mCustomTimeJson.setTuesdayMinute(minute);
        addValue(getKey() + "/tuesdayMinute", minute);
    }

    public void setWednesdayHour(int hour) {
        if (getWednesdayHour() == hour)
            return;

        mCustomTimeJson.setWednesdayHour(hour);
        addValue(getKey() + "/wednesdayHour", hour);
    }

    public void setWednesdayMinute(int minute) {
        if (getWednesdayMinute() == minute)
            return;

        mCustomTimeJson.setWednesdayMinute(minute);
        addValue(getKey() + "/wednesdayMinute", minute);
    }

    public void setThursdayHour(int hour) {
        if (getThursdayHour() == hour)
            return;

        mCustomTimeJson.setThursdayHour(hour);
        addValue(getKey() + "/thursdayHour", hour);
    }

    public void setThursdayMinute(int minute) {
        if (getThursdayMinute() == minute)
            return;

        mCustomTimeJson.setThursdayMinute(minute);
        addValue(getKey() + "/thursdayMinute", minute);
    }

    public void setFridayHour(int hour) {
        if (getFridayHour() == hour)
            return;

        mCustomTimeJson.setFridayHour(hour);
        addValue(getKey() + "/fridayHour", hour);
    }

    public void setFridayMinute(int minute) {
        if (getFridayMinute() == minute)
            return;

        mCustomTimeJson.setFridayMinute(minute);
        addValue(getKey() + "/fridayMinute", minute);
    }

    public void setSaturdayHour(int hour) {
        if (getSaturdayHour() == hour)
            return;

        mCustomTimeJson.setSaturdayHour(hour);
        addValue(getKey() + "/saturdayHour", hour);
    }

    public void setSaturdayMinute(int minute) {
        if (getSundayMinute() == minute)
            return;

        mCustomTimeJson.setSaturdayMinute(minute);
        addValue(getKey() + "/saturdayMinute", minute);
    }
}
