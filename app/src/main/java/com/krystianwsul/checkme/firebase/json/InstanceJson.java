package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import junit.framework.Assert;

public class InstanceJson {
    private String taskId;

    private Long done;

    private int scheduleYear;
    private int scheduleMonth;
    private int scheduleDay;

    private Integer scheduleCustomTimeId;

    private Integer scheduleHour;
    private Integer scheduleMinute;

    private Integer instanceYear;
    private Integer instanceMonth;
    private Integer instanceDay;

    private Integer instanceCustomTimeId;

    private Integer instanceHour;
    private Integer instanceMinute;

    private long hierarchyTime;

    public InstanceJson() {

    }

    InstanceJson(@NonNull String taskId, @Nullable Long done, int scheduleYear, int scheduleMonth, int scheduleDay, @Nullable Integer scheduleCustomTimeId, @Nullable Integer scheduleHour, @Nullable Integer scheduleMinute, @Nullable Integer instanceYear, @Nullable Integer instanceMonth, @Nullable Integer instanceDay, @Nullable Integer instanceCustomTimeId, @Nullable Integer instanceHour, @Nullable Integer instanceMinute, long hierarchyTime) {
        Assert.assertTrue((scheduleHour == null) == (scheduleMinute == null));
        Assert.assertTrue((scheduleHour == null) != (scheduleCustomTimeId == null));

        Assert.assertTrue((instanceYear == null) == (instanceMonth == null));
        Assert.assertTrue((instanceYear == null) == (instanceDay == null));
        boolean hasInstanceDate = (instanceYear != null);

        Assert.assertTrue((instanceHour == null) == (instanceMinute == null));
        Assert.assertTrue((instanceHour == null) || (instanceCustomTimeId == null));
        boolean hasInstanceTime = ((instanceHour != null) || (instanceCustomTimeId != null));
        Assert.assertTrue(hasInstanceDate == hasInstanceTime);

        this.taskId = taskId;

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

    @NonNull
    public String getTaskId() {
        return taskId;
    }

    public Long getDone() {
        return done;
    }

    public int getScheduleYear() {
        return scheduleYear;
    }

    public int getScheduleMonth() {
        return scheduleMonth;
    }

    public int getScheduleDay() {
        return scheduleDay;
    }

    @Nullable
    public Integer getScheduleCustomTimeId() {
        return scheduleCustomTimeId;
    }

    @Nullable
    public Integer getScheduleHour() {
        return scheduleHour;
    }

    @Nullable
    public Integer getScheduleMinute() {
        return scheduleMinute;
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
    public Integer getInstanceCustomTimeId() {
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
}
