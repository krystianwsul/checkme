package com.krystianwsul.checkme.domainmodel.local;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.MonthlyDayScheduleBridge;
import com.krystianwsul.checkme.persistencemodel.MonthlyDayScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;

class LocalMonthlyDayScheduleBridge extends LocalScheduleBridge implements MonthlyDayScheduleBridge {
    @NonNull
    private final MonthlyDayScheduleRecord mMonthlyDayScheduleRecord;

    LocalMonthlyDayScheduleBridge(@NonNull ScheduleRecord scheduleRecord, @NonNull MonthlyDayScheduleRecord monthlyDayScheduleRecord) {
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
    public CustomTimeKey getCustomTimeKey() {
        if (mMonthlyDayScheduleRecord.getCustomTimeId() != null)
            return new CustomTimeKey(mMonthlyDayScheduleRecord.getCustomTimeId());
        else
            return null;
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
        getScheduleRecord().delete();
        mMonthlyDayScheduleRecord.delete();
    }
}
