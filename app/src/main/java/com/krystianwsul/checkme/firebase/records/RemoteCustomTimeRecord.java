package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.json.CustomTimeJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;

import junit.framework.Assert;

public class RemoteCustomTimeRecord extends RemoteRecord {
    RemoteCustomTimeRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteCustomTimeRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    @NonNull
    private CustomTimeJson getCustomTimeJson() {
        CustomTimeJson customTimeJson = mJsonWrapper.customTimeJson;
        Assert.assertTrue(customTimeJson != null);

        return customTimeJson;
    }

    @NonNull
    public String getOwnerId() {
        return getCustomTimeJson().getOwnerId();
    }

    public int getLocalId() {
        return getCustomTimeJson().getLocalId();
    }

    @NonNull
    public String getName() {
        return getCustomTimeJson().getName();
    }

    public int getSundayHour() {
        return getCustomTimeJson().getSundayHour();
    }

    public int getSundayMinute() {
        return getCustomTimeJson().getSundayMinute();
    }

    public int getMondayHour() {
        return getCustomTimeJson().getMondayHour();
    }

    public int getMondayMinute() {
        return getCustomTimeJson().getMondayMinute();
    }

    public int getTuesdayHour() {
        return getCustomTimeJson().getTuesdayHour();
    }

    public int getTuesdayMinute() {
        return getCustomTimeJson().getTuesdayMinute();
    }

    public int getWednesdayHour() {
        return getCustomTimeJson().getWednesdayHour();
    }

    public int getWednesdayMinute() {
        return getCustomTimeJson().getWednesdayMinute();
    }

    public int getThursdayHour() {
        return getCustomTimeJson().getThursdayHour();
    }

    public int getThursdayMinute() {
        return getCustomTimeJson().getThursdayMinute();
    }

    public int getFridayHour() {
        return getCustomTimeJson().getFridayHour();
    }

    public int getFridayMinute() {
        return getCustomTimeJson().getFridayMinute();
    }

    public int getSaturdayHour() {
        return getCustomTimeJson().getSaturdayHour();
    }

    public int getSaturdayMinute() {
        return getCustomTimeJson().getSaturdayMinute();
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (getName().equals(name))
            return;

        getCustomTimeJson().setName(name);
        addValue(getId() + "/customTimeJson/name", name);
    }

    public void setSundayHour(int hour) {
        if (getSundayHour() == hour)
            return;

        getCustomTimeJson().setSundayHour(hour);
        addValue(getId() + "/customTimeJson/sundayHour", hour);
    }

    public void setSundayMinute(int minute) {
        if (getSundayMinute() == minute)
            return;

        getCustomTimeJson().setSundayMinute(minute);
        addValue(getId() + "/customTimeJson/sundayMinute", minute);
    }

    public void setMondayHour(int hour) {
        if (getMondayHour() == hour)
            return;

        getCustomTimeJson().setMondayHour(hour);
        addValue(getId() + "/customTimeJson/mondayHour", hour);
    }

    public void setMondayMinute(int minute) {
        if (getMondayMinute() == minute)
            return;

        getCustomTimeJson().setMondayMinute(minute);
        addValue(getId() + "/customTimeJson/mondayMinute", minute);
    }

    public void setTuesdayHour(int hour) {
        if (getTuesdayHour() == hour)
            return;

        getCustomTimeJson().setTuesdayHour(hour);
        addValue(getId() + "/customTimeJson/tuesdayHour", hour);
    }

    public void setTuesdayMinute(int minute) {
        if (getTuesdayMinute() == minute)
            return;

        getCustomTimeJson().setTuesdayMinute(minute);
        addValue(getId() + "/customTimeJson/tuesdayMinute", minute);
    }

    public void setWednesdayHour(int hour) {
        if (getWednesdayHour() == hour)
            return;

        getCustomTimeJson().setWednesdayHour(hour);
        addValue(getId() + "/customTimeJson/wednesdayHour", hour);
    }

    public void setWednesdayMinute(int minute) {
        if (getWednesdayMinute() == minute)
            return;

        getCustomTimeJson().setWednesdayMinute(minute);
        addValue(getId() + "/customTimeJson/wednesdayMinute", minute);
    }

    public void setThursdayHour(int hour) {
        if (getThursdayHour() == hour)
            return;

        getCustomTimeJson().setThursdayHour(hour);
        addValue(getId() + "/customTimeJson/thursdayHour", hour);
    }

    public void setThursdayMinute(int minute) {
        if (getThursdayMinute() == minute)
            return;

        getCustomTimeJson().setThursdayMinute(minute);
        addValue(getId() + "/customTimeJson/thursdayMinute", minute);
    }

    public void setFridayHour(int hour) {
        if (getFridayHour() == hour)
            return;

        getCustomTimeJson().setFridayHour(hour);
        addValue(getId() + "/customTimeJson/fridayHour", hour);
    }

    public void setFridayMinute(int minute) {
        if (getFridayMinute() == minute)
            return;

        getCustomTimeJson().setFridayMinute(minute);
        addValue(getId() + "/customTimeJson/fridayMinute", minute);
    }

    public void setSaturdayHour(int hour) {
        if (getSaturdayHour() == hour)
            return;

        getCustomTimeJson().setSaturdayHour(hour);
        addValue(getId() + "/customTimeJson/saturdayHour", hour);
    }

    public void setSaturdayMinute(int minute) {
        if (getSundayMinute() == minute)
            return;

        getCustomTimeJson().setSaturdayMinute(minute);
        addValue(getId() + "/customTimeJson/saturdayMinute", minute);
    }
}
