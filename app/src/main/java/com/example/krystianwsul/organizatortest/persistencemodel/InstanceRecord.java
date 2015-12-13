package com.example.krystianwsul.organizatortest.persistencemodel;

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

    private final Integer mInstanceYear;
    private final Integer mInstanceMonth;
    private final Integer mInstanceDay;

    private final Integer mInstanceCustomTimeId;

    private final Integer mInstanceHour;
    private final Integer mInstanceMinute;

    public InstanceRecord(int id, int taskId, Long done, int scheduleYear, int scheduleMonth, int scheduleDay, Integer scheduleCustomTimeId, Integer scheduleHour, Integer scheduleMinute, Integer instanceYear, Integer instanceMonth, Integer instanceDay, Integer instanceCustomTimeId, Integer instanceHour, Integer instanceMinute) {
        Assert.assertTrue((scheduleHour == null) == (scheduleMinute == null));
        Assert.assertTrue((scheduleHour == null) != (scheduleCustomTimeId == null));

        Assert.assertTrue((instanceYear == null) == (instanceMonth == null) == (instanceDay == null));
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

    public Integer getInstanceMonth() {
        return mInstanceMonth;
    }

    public Integer getInstanceDay() {
        return mInstanceDay;
    }

    public Integer getInstanceCustomTimeId() {
        return mInstanceCustomTimeId;
    }

    public Integer getInstanceHour() {
        return mInstanceHour;
    }

    public Integer getInstanceMinute() {
        return mInstanceMinute;
    }

    public void setDone(Long done) {
        mDone = done;
    }
}
