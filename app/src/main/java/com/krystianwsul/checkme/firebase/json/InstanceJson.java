package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.Nullable;

import junit.framework.Assert;

public class InstanceJson {
    private Long done;

    private int scheduleYear; // todo remove
    private int scheduleMonth;
    private int scheduleDay;

    private String scheduleCustomTimeId;

    private Integer scheduleHour;
    private Integer scheduleMinute;

    private Integer instanceYear;
    private Integer instanceMonth;
    private Integer instanceDay;

    private String instanceCustomTimeId;

    private Integer instanceHour;
    private Integer instanceMinute;

    private long hierarchyTime;

    @SuppressWarnings("unused")
    public InstanceJson() {

    }

    public InstanceJson(@Nullable Long done, int scheduleYear, int scheduleMonth, int scheduleDay, @Nullable String scheduleCustomTimeId, @Nullable Integer scheduleHour, @Nullable Integer scheduleMinute, @Nullable Integer instanceYear, @Nullable Integer instanceMonth, @Nullable Integer instanceDay, @Nullable String instanceCustomTimeId, @Nullable Integer instanceHour, @Nullable Integer instanceMinute, long hierarchyTime) {
        Assert.assertTrue((scheduleHour == null) == (scheduleMinute == null));
        Assert.assertTrue((scheduleHour == null) != (scheduleCustomTimeId == null));

        Assert.assertTrue((instanceYear == null) == (instanceMonth == null));
        Assert.assertTrue((instanceYear == null) == (instanceDay == null));
        boolean hasInstanceDate = (instanceYear != null);

        Assert.assertTrue((instanceHour == null) == (instanceMinute == null));
        Assert.assertTrue((instanceHour == null) || (instanceCustomTimeId == null));
        boolean hasInstanceTime = ((instanceHour != null) || (instanceCustomTimeId != null));
        Assert.assertTrue(hasInstanceDate == hasInstanceTime);

        this.done = done;

        this.scheduleYear = scheduleYear;
        this.scheduleMonth = scheduleMonth;
        this.scheduleDay = scheduleDay;

        this.scheduleCustomTimeId = scheduleCustomTimeId;

        this.scheduleHour = scheduleHour;
        this.scheduleMinute = scheduleMinute;

        this.instanceYear = instanceYear;
        this.instanceMonth = instanceMonth;
        this.instanceDay = instanceDay;

        this.instanceCustomTimeId = instanceCustomTimeId;

        this.instanceHour = instanceHour;
        this.instanceMinute = instanceMinute;

        this.hierarchyTime = hierarchyTime;
    }

    public Long getDone() {
        return done;
    }

    @Nullable
    public Integer getInstanceYear() {
        return instanceYear;
    }

    @Nullable
    public Integer getInstanceMonth() {
        return instanceMonth;
    }

    @Nullable
    public Integer getInstanceDay() {
        return instanceDay;
    }

    @Nullable
    public String getInstanceCustomTimeId() {
        return instanceCustomTimeId;
    }

    @Nullable
    public Integer getInstanceHour() {
        return instanceHour;
    }

    @Nullable
    public Integer getInstanceMinute() {
        return instanceMinute;
    }

    public long getHierarchyTime() {
        return hierarchyTime;
    }

    public void setInstanceYear(int instanceYear) {
        this.instanceYear = instanceYear;
    }

    public void setInstanceMonth(int instanceMonth) {
        this.instanceMonth = instanceMonth;
    }

    public void setInstanceDay(int instanceDay) {
        this.instanceDay = instanceDay;
    }

    public void setInstanceCustomTimeId(@Nullable String instanceCustomTimeId) {
        this.instanceCustomTimeId = instanceCustomTimeId;
    }

    public void setInstanceHour(@Nullable Integer instanceHour) {
        this.instanceHour = instanceHour;
    }

    public void setInstanceMinute(@Nullable Integer instanceMinute) {
        this.instanceMinute = instanceMinute;
    }

    public void setDone(@Nullable Long done) {
        this.done = done;
    }
}
