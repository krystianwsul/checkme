package com.krystianwsul.checkme.domainmodel.local;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.WeeklyScheduleRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;

class LocalWeeklyScheduleBridge extends LocalScheduleBridge implements WeeklyScheduleBridge {
    @NonNull
    private final WeeklyScheduleRecord mWeeklyScheduleRecord;

    LocalWeeklyScheduleBridge(@NonNull ScheduleRecord scheduleRecord, @NonNull WeeklyScheduleRecord weeklyScheduleRecord) {
        super(scheduleRecord);

        mWeeklyScheduleRecord = weeklyScheduleRecord;
    }

    @Override
    public int getDayOfWeek() {
        return mWeeklyScheduleRecord.getDayOfWeek();
    }

    @Nullable
    @Override
    public CustomTimeKey getCustomTimeKey() {
        if (mWeeklyScheduleRecord.getCustomTimeId() != null)
            return new CustomTimeKey(mWeeklyScheduleRecord.getCustomTimeId());
        else
            return null;
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
