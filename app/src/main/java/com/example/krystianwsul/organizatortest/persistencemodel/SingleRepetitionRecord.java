package com.example.krystianwsul.organizatortest.persistencemodel;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/14/2015.
 */
public class SingleRepetitionRecord {
    private final int mRootTaskId;

    private final Integer mRepetitionYear;
    private final Integer mRepetitionMonth;
    private final Integer mRepetitionDay;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    public SingleRepetitionRecord(int rootTaskId, Integer repetitionYear, Integer repetitionMonth, Integer repetitionDay, Integer customTimeId, Integer hour, Integer minute) {
        Assert.assertTrue((repetitionYear == null) == (repetitionMonth == null) == (repetitionDay == null));
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));

        mRootTaskId = rootTaskId;

        mRepetitionYear = repetitionYear;
        mRepetitionMonth = repetitionMonth;
        mRepetitionDay = repetitionDay;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getRootTaskId() {
        return mRootTaskId;
    }

    public Integer getRepetitionYear() {
        return mRepetitionYear;
    }

    public Integer getRepetitionMonth() {
        return mRepetitionMonth;
    }

    public Integer getRepetitionDay() {
        return mRepetitionDay;
    }

    public Integer getCustomTimeId() {
        return mCustomTimeId;
    }

    public Integer getHour() {
        return mHour;
    }

    public Integer getMinute() {
        return mMinute;
    }
}
