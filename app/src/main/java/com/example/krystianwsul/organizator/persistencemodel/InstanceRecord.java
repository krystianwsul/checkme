package com.example.krystianwsul.organizator.persistencemodel;

import junit.framework.Assert;

public class InstanceRecord {
    private final int mId;
    private final int mTaskId;

    private Long mDone;

    private final int mScheduleYear;
    private final int mScheduleMonth;
    private final int mScheduleDay;

    private final Integer mScheduleCustomTimeId;

    private final Integer mScheduleHour;
    private final Integer mScheduleMinute;

    private Integer mInstanceYear;
    private Integer mInstanceMonth;
    private Integer mInstanceDay;

    private Integer mInstanceCustomTimeId;

    private Integer mInstanceHour;
    private Integer mInstanceMinute;

    private final long mHierarchyTime;

    public InstanceRecord(int id, int taskId, Long done, int scheduleYear, int scheduleMonth, int scheduleDay, Integer scheduleCustomTimeId, Integer scheduleHour, Integer scheduleMinute, Integer instanceYear, Integer instanceMonth, Integer instanceDay, Integer instanceCustomTimeId, Integer instanceHour, Integer instanceMinute, long hierarchyTime) {
        Assert.assertTrue((scheduleHour == null) == (scheduleMinute == null));
        Assert.assertTrue((scheduleHour == null) != (scheduleCustomTimeId == null));

        Assert.assertTrue((instanceYear == null) == (instanceMonth == null));
        Assert.assertTrue((instanceYear == null) == (instanceDay == null));
        boolean hasInstanceDate = (instanceYear != null);

        Assert.assertTrue((instanceHour == null) == (instanceMinute == null));
        Assert.assertTrue((instanceHour == null) || (instanceCustomTimeId == null));
        boolean hasInstanceTime = ((instanceHour != null) || (instanceCustomTimeId != null));
        Assert.assertTrue(hasInstanceDate == hasInstanceTime);

        mId = id;
        mTaskId = taskId;

        mDone = done;

        mScheduleYear = scheduleYear;
        mScheduleMonth = scheduleMonth;
        mScheduleDay = scheduleDay;

        mScheduleCustomTimeId = scheduleCustomTimeId;

        mScheduleHour = scheduleHour;
        mScheduleMinute = scheduleMinute;

        mInstanceYear = instanceYear;
        mInstanceMonth = instanceMonth;
        mInstanceDay = instanceDay;

        mInstanceCustomTimeId = instanceCustomTimeId;

        mInstanceHour = instanceHour;
        mInstanceMinute = instanceMinute;

        mHierarchyTime = hierarchyTime;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public Long getDone() {
        return mDone;
    }

    public int getScheduleYear() {
        return mScheduleYear;
    }

    public int getScheduleMonth() {
        return mScheduleMonth;
    }

    public int getScheduleDay() {
        return mScheduleDay;
    }

    public Integer getScheduleCustomTimeId() {
        return mScheduleCustomTimeId;
    }

    public Integer getScheduleHour() {
        return mScheduleHour;
    }

    public Integer getScheduleMinute() {
        return mScheduleMinute;
    }

    public Integer getInstanceYear() {
        return mInstanceYear;
    }

    public void setInstanceYear(int instanceYear) {
        mInstanceYear = instanceYear;
    }

    public Integer getInstanceMonth() {
        return mInstanceMonth;
    }

    public void setInstanceMonth(int instanceMonth) {
        mInstanceMonth = instanceMonth;
    }

    public Integer getInstanceDay() {
        return mInstanceDay;
    }

    public void setInstanceDay(int instanceDay) {
        mInstanceDay = instanceDay;
    }

    public Integer getInstanceCustomTimeId() {
        return mInstanceCustomTimeId;
    }

    public void setInstanceCustomTimeId(int instanceCustomTimeId) {
        mInstanceCustomTimeId = instanceCustomTimeId;
    }

    public Integer getInstanceHour() {
        return mInstanceHour;
    }

    public void setInstanceHour(int instanceHour) {
        mInstanceHour = instanceHour;
    }

    public Integer getInstanceMinute() {
        return mInstanceMinute;
    }

    public void setInstanceMinute(int instanceMinute) {
        mInstanceMinute = instanceMinute;
    }

    public void setDone(Long done) {
        mDone = done;
    }

    public long getHierarchyTime() {
        return mHierarchyTime;
    }
}
