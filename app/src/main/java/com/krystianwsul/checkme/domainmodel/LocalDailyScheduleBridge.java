package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.DailyScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;

public class LocalDailyScheduleBridge extends LocalScheduleBridge implements DailyScheduleBridge {
    @NonNull
    private final DailyScheduleRecord mDailyScheduleRecord;

    public LocalDailyScheduleBridge(@NonNull ScheduleRecord scheduleRecord, @NonNull DailyScheduleRecord dailyScheduleRecord) {
        super(scheduleRecord);

        mDailyScheduleRecord = dailyScheduleRecord;
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mDailyScheduleRecord.getCustomTimeId();
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mDailyScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mDailyScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mScheduleRecord.delete();
        mDailyScheduleRecord.delete();
    }
}
