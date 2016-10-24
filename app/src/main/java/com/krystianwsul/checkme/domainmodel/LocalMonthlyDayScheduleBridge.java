package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.MonthlyDayScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;

public class LocalMonthlyDayScheduleBridge extends LocalScheduleBridge implements MonthlyDayScheduleBridge {
    @NonNull
    private final MonthlyDayScheduleRecord mMonthlyDayScheduleRecord;

    public LocalMonthlyDayScheduleBridge(@NonNull ScheduleRecord scheduleRecord, @NonNull MonthlyDayScheduleRecord monthlyDayScheduleRecord) {
        super(scheduleRecord);

        mMonthlyDayScheduleRecord = monthlyDayScheduleRecord;
    }

    @Override
    public int getDayOfMonth() {
        return mMonthlyDayScheduleRecord.getDayOfMonth();
    }

    @Override
    public boolean getBeginningOfMonth() {
        return mMonthlyDayScheduleRecord.getBeginningOfMonth();
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mMonthlyDayScheduleRecord.getCustomTimeId();
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mMonthlyDayScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mMonthlyDayScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mScheduleRecord.delete();
        mMonthlyDayScheduleRecord.delete();
    }
}
