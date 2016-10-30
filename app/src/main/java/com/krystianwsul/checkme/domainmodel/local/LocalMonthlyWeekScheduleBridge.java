package com.krystianwsul.checkme.domainmodel.local;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.MonthlyWeekScheduleBridge;
import com.krystianwsul.checkme.persistencemodel.MonthlyWeekScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;

class LocalMonthlyWeekScheduleBridge extends LocalScheduleBridge implements MonthlyWeekScheduleBridge {
    @NonNull
    private final MonthlyWeekScheduleRecord mMonthlyWeekScheduleRecord;

    LocalMonthlyWeekScheduleBridge(@NonNull ScheduleRecord scheduleRecord, @NonNull MonthlyWeekScheduleRecord monthlyWeekScheduleRecord) {
        super(scheduleRecord);

        mMonthlyWeekScheduleRecord = monthlyWeekScheduleRecord;
    }

    @Override
    public int getDayOfMonth() {
        return mMonthlyWeekScheduleRecord.getDayOfMonth();
    }

    @Override
    public int getDayOfWeek() {
        return mMonthlyWeekScheduleRecord.getDayOfWeek();
    }

    @Override
    public boolean getBeginningOfMonth() {
        return mMonthlyWeekScheduleRecord.getBeginningOfMonth();
    }

    @Nullable
    @Override
    public CustomTimeKey getCustomTimeKey() {
        if (mMonthlyWeekScheduleRecord.getCustomTimeId() != null)
            return new CustomTimeKey(mMonthlyWeekScheduleRecord.getCustomTimeId());
        else
            return null;
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mMonthlyWeekScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mMonthlyWeekScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mScheduleRecord.delete();
        mMonthlyWeekScheduleRecord.delete();
    }
}
