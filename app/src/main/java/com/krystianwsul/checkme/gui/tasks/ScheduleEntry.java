package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;

import java.util.Map;

public abstract class ScheduleEntry implements Parcelable {
    @Nullable
    String mError;

    static ScheduleEntry fromScheduleDialogData(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        switch (scheduleDialogData.getMScheduleType()) {
            case SINGLE:
                return new SingleScheduleEntry(scheduleDialogData);
            case WEEKLY:
                return new WeeklyScheduleEntry(scheduleDialogData);
            case MONTHLY_DAY:
                return new MonthlyDayScheduleEntry(scheduleDialogData);
            case MONTHLY_WEEK:
                return new MonthlyWeekScheduleEntry(scheduleDialogData);
            default:
                throw new UnsupportedOperationException();
        }
    }

    ScheduleEntry() {
        mError = null;
    }

    ScheduleEntry(@Nullable String error) {
        mError = error;
    }

    @NonNull
    abstract String getText(@NonNull Map<CustomTimeKey, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context);

    @NonNull
    abstract CreateTaskLoader.ScheduleData getScheduleData();

    @NonNull
    abstract ScheduleDialogFragment.ScheduleDialogData getScheduleDialogData(@NonNull Date today, @Nullable CreateTaskActivity.ScheduleHint scheduleHint);

    @NonNull
    abstract ScheduleType getScheduleType();
}
