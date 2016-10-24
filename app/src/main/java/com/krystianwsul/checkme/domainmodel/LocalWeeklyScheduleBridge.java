package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.WeeklyScheduleRecord;

public class LocalWeeklyScheduleBridge extends LocalScheduleBridge implements WeeklyScheduleBridge {
    @NonNull
    private final WeeklyScheduleRecord mWeeklyScheduleRecord;

    public LocalWeeklyScheduleBridge(@NonNull ScheduleRecord scheduleRecord, @NonNull WeeklyScheduleRecord weeklyScheduleRecord) {
        super(scheduleRecord);

        mWeeklyScheduleRecord = weeklyScheduleRecord;
    }

    @Override
    public int getDayOfWeek() {
        return mWeeklyScheduleRecord.getDayOfWeek();
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mWeeklyScheduleRecord.getCustomTimeId();
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mWeeklyScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mWeeklyScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mScheduleRecord.delete();
        mWeeklyScheduleRecord.delete();
    }
}
